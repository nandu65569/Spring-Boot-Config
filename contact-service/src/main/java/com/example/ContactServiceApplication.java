package com.example;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.Collection;

@SpringCloudApplication
public class ContactServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner init(ContactRepository contactRepository) {
		return args ->
				Arrays.asList("Google,Amazon,Yahoo,Myntra,Flipkart".split(",")).forEach(
						userId ->
								Arrays.asList("Dave,Syer;Phil,Webb;Juergen,Hoeller".split(";"))
										.stream()
										.map(n -> n.split(","))
										.forEach(name ->
												contactRepository.save(new Contact(
												userId, name[0], name[1], name[0].toLowerCase() + "@email.com"))));
	}
}

interface ContactRepository extends JpaRepository<Contact, Long> {
	Collection<Contact> findByUserId(String userId);
}

@RestController
class ContactRestController {
	@Autowired
	private ContactRepository contactRepository;

    public Collection<Contact> contactsFallBack(String userId){
		System.out.println("contactsFallBack");
        return Arrays.asList();
    }

	@HystrixCommand(fallbackMethod = "contactsFallBack")
	@RequestMapping("/{userId}/contacts")
	public Collection<Contact> contacts(@PathVariable String userId) {
		return this.contactRepository.findByUserId(userId);
	}
}

@Entity
class Contact {

	@Id
	@GeneratedValue
	private Long id;
	private String userId, firstName, lastName, email;

	public Contact() {
	}

	public Contact(String userId, String firstName, String lastName, String email) {
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

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
