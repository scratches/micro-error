package com.example.demo;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@AutoConfigureWebTestClient
public class ErrorApplicationTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void get() throws Exception {
		client.get().uri("/").exchange().expectBody(String.class).isEqualTo("Hello");
	}

	@Test
	public void bad() throws Exception {
		client.get().uri("/bad").exchange().expectStatus().isNotFound()
				.expectBody(String.class).value(containsString("status"));
	}

	@Test
	public void boom() throws Exception {
		client.get().uri("/boom").exchange().expectStatus().is5xxServerError()
				.expectBody(String.class).value(containsString("status"));
	}
}
