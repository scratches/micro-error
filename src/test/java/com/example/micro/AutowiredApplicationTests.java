package com.example.micro;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=MicroApplication.class)
@DirtiesContext
@AutoConfigureWebTestClient
public class AutowiredApplicationTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void test() {
		client.get().uri("/").exchange().expectBody(String.class).isEqualTo("Hello");
	}

	@Test
	public void bad() throws Exception {
		client.get().uri("/bad").exchange().expectStatus().isNotFound()
				.expectBody(String.class).value(nullValue(String.class));
	}

	@Test
	public void boom() throws Exception {
		client.get().uri("/boom").exchange().expectStatus().is5xxServerError()
				.expectBody(String.class).value(containsString("status"));
	}

}
