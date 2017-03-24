package com.example;

import com.netflix.client.config.IClientConfig;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingUrl;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SpringCloudApplication
@EnableFeignClients
@EnableZuulProxy
@RibbonClient(name = "bookmark-service", configuration = BookmarkConfiguration.class)
public class PassportServiceApplication {

	@LoadBalanced
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	/*@Order(2)
	@Bean
	CommandLineRunner dc(DiscoveryClient discoveryClient){
		return args -> discoveryClient.getInstances("bookmark-service").forEach(si -> {
			System.out.println(String.format("(%s) %s:%s",si.getServiceId(),si.getHost(),si.getPort()));
		});
	}

	@Order(1)
	@Bean
	CommandLineRunner dc1(DiscoveryClient discoveryClient){
		return args -> discoveryClient.getInstances("contact-service").forEach(si -> {
			System.out.println(String.format("(%s) %s:%s",si.getServiceId(),si.getHost(),si.getPort()));
		});
	}*/


	public static void main(String[] args) {
		SpringApplication.run(PassportServiceApplication.class, args);
	}
}

@FeignClient("BOOKMARK-SERVICE")
interface BookmarkClient {
	@RequestMapping(method = RequestMethod.GET, value = "/{userId}/bookmarks")
	Collection<Bookmark> getBookmarks(@PathVariable("userId") String userId);
}

@FeignClient("CONTACT-SERVICE")
interface ContactClient {
	@RequestMapping(method = RequestMethod.GET, value = "/{userId}/contacts")
	Collection<Contact> getContacts(@PathVariable("userId") String userId);
}

@Order(1)
@Component
class DiscoveryClientExample implements CommandLineRunner {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Override
	public void run(String... strings) throws Exception {
		System.out.println("------------------------------");
		System.out.println("DiscoveryClient Example");
		System.out.println("Order 1 : ");
		discoveryClient.getInstances("contact-service").forEach((ServiceInstance s) -> {
			System.out.println(String.format("(%s) %s:%s",s.getHost(),s.getUri(),s.getPort()));
		});
		discoveryClient.getInstances("bookmark-service").forEach((ServiceInstance s) -> {
			System.out.println(String.format("(%s) %s:%s",s.getHost(),s.getUri(),s.getPort()));
		});
	}
}

@Order(2)
@Component
class RestTemplateExample implements CommandLineRunner {

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public void run(String... strings) throws Exception {
		System.out.println("------------------------------");
		System.out.println("RestTemplate Example");
		System.out.println("Order 2 @ RestTemplate : ");

		ParameterizedTypeReference<List<Bookmark>> bookmarksResponseType =
				new ParameterizedTypeReference<List<Bookmark>>() {};

		ParameterizedTypeReference<List<Contact>> contactsResponseType =
				new ParameterizedTypeReference<List<Contact>>() {};

		ResponseEntity<List<Bookmark>> bookmarks = this.restTemplate.exchange(
				"http://BOOKMARK-SERVICE/{userId}/bookmarks",
				HttpMethod.GET, null, bookmarksResponseType, (Object) "Myntra");
		bookmarks.getBody().forEach(System.out::println);

		System.out.println("------------------------------");

		ResponseEntity<List<Contact>> contacts = this.restTemplate.exchange(
				"http://CONTACT-SERVICE/{userId}/contacts",
				HttpMethod.GET, null, contactsResponseType, (Object) "Myntra");
		contacts.getBody().forEach(System.out::println);
	}
}

@Order(3)
@Component
class FeignClientExample implements CommandLineRunner {

	@Autowired
	private ContactClient contactClient;

	@Autowired
	private BookmarkClient bookmarkClient;

	@Override
	public void run(String... strings) throws Exception {

		System.out.println("------------------------------");
		System.out.println("Feign Example");
		System.out.println("Order 3 @ Feign : ");
		this.bookmarkClient.getBookmarks("Flipkart").forEach(System.out::println);
		this.contactClient.getContacts("Flipkart").forEach(System.out::println);
	}
}



@Component
class ServicesRepo {

	@Autowired
	private ContactClient contactClient;

	@Autowired
	private BookmarkClient bookmarkClient;

	public Collection<Bookmark> getBookmarksFallback(String userId) {
		System.out.println("getBookmarksFallback");
		return Arrays.asList();
	}

	@HystrixCommand(fallbackMethod = "getBookmarksFallback")
	public Collection<Bookmark> getBookmarks(String userId) {
		return this.bookmarkClient.getBookmarks(userId);
	}

	public Collection<Contact> getContactsFallback(String userId) {
		System.out.println("getContactsFallback");
		return Arrays.asList();
	}

	@HystrixCommand(fallbackMethod = "getContactsFallback")
	public Collection<Contact> getContacts(String userId) {
		return this.contactClient.getContacts(userId);
	}
}


@RestController
class PassportRestController {

	@Autowired
	private ServicesRepo servicesRepo;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	DiscoveryClient client;

	@RequestMapping("/{userId}/passport")
	Passport passport(@PathVariable String userId) {
		return new Passport(userId,servicesRepo.getContacts(userId),servicesRepo.getBookmarks(userId));
	}

	@RequestMapping("/bookmarks")
	public String allBookMarks(@RequestParam(value="name", defaultValue="Google") String name) {
		String instance= restTemplate.getForObject("http://BOOKMARK-SERVICE/instance", String.class);
		return instance;
	}

}

class Passport {
	private String userId;
	private Collection<Bookmark> bookmarks;
	private Collection<Contact> contacts;

	public Passport(){}

	public Passport(String userId,
					Collection<Contact> contacts,
					Collection<Bookmark> bookmarks) {
		this.userId = userId;
		this.bookmarks = bookmarks;
		this.contacts = contacts;
	}

	@Override
	public String toString() {
		return "Passport{" +
				"userId='" + userId + '\'' +
				", bookmarks=" + bookmarks +
				", contacts=" + contacts +
				'}';
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Collection<Bookmark> getBookmarks() {
		return bookmarks;
	}

	public void setBookmarks(Collection<Bookmark> bookmarks) {
		this.bookmarks = bookmarks;
	}

	public Collection<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(Collection<Contact> contacts) {
		this.contacts = contacts;
	}
}


class Contact {
	private Long id;
	private String userId, firstName, lastName, email;

	@Override
	public String toString() {
		return "Contact{" +
				"id=" + id +
				", userId='" + userId + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", email='" + email + '\'' +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}


class Bookmark {
	private Long id;
	private String href, description, userId;

	public Bookmark() {
	}

	@Override
	public String toString() {
		return "Bookmark{" +
				"id=" + id +
				", href='" + href + '\'' +
				", description='" + description + '\'' +
				", userId='" + userId + '\'' +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}

class BookmarkConfiguration {

	@Autowired
	IClientConfig ribbonClientConfig;

	@Bean
	public IPing ribbonPing(IClientConfig config) {
		return new PingUrl();
	}

	@Bean
	public IRule ribbonRule(IClientConfig config) {
		return new AvailabilityFilteringRule();
	}

}