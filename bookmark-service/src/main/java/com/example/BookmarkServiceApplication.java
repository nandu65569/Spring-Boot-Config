package com.example;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.awt.print.Book;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

@SpringCloudApplication
public class BookmarkServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookmarkServiceApplication.class, args);
	}

	private String descriptionForBookmark(String mask, String userId, String href) {
		return mask.replaceFirst("_L_", href)
				.replaceFirst("_U_", userId);
	}

	@Bean
	CommandLineRunner init(@Value("${bookmark.mask}") String bookmarkMask, BookmarkRepository bookmarkRepository) {
		return args ->
				Arrays.asList("Google,Amazon,Yahoo,Myntra,Flipkart".split(",")).forEach(userId -> {
					String href = String.format("http://%s-link.com", userId);
					String descriptionForBookmark = this.descriptionForBookmark(bookmarkMask, userId, href);
					System.out.println("descriptionForBookmark : "+ descriptionForBookmark);
					bookmarkRepository.save(new Bookmark(href, userId, descriptionForBookmark));
				});
	}
}


@RefreshScope
@RestController
class MessageRestController {

	@Value("${message}")
	private String message;

	@RequestMapping("/message")
	String msg() {
		return this.message;
	}

}

interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
	Collection<Bookmark> findByUserId(String userId);
}

@RestController
class BookmarkRestController {

	@Autowired
	private BookmarkRepository bookmarkRepository;


	private Collection<Bookmark> bookmarksFallback(String userId){
		System.out.println("bookmarksFallback");
		return Arrays.asList();
	}

	@HystrixCommand(fallbackMethod = "bookmarksFallback")
	@RequestMapping("/{userId}/bookmarks")
	Collection<Bookmark> bookmarks(@PathVariable String userId) {
		return this.bookmarkRepository.findByUserId(userId);
	}
}

@Entity
class Bookmark {

	@Id
	@GeneratedValue
	private Long id;
	private String href, userId, description;

	Bookmark() {
	}

	public Bookmark(
			String href,
			String userId,
			String description) {

		this.href = href;
		this.userId = userId;
		this.description = description;
	}

	@Override
	public String toString() {
		return "Bookmark{" +
				"id=" + id +
				", href='" + href + '\'' +
				", userId='" + userId + '\'' +
				", description='" + description + '\'' +
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
