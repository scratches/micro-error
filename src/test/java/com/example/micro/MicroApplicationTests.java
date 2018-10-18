package com.example.micro;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

public class MicroApplicationTests {

	private WebTestClient client;

	private ConfigurableApplicationContext context;

	@Before
	public void init() {
		context = new MicroApplication().run("--server.port=-1");
		WebHandler webHandler = context.getBean(WebHandler.class);
		client = WebTestClient.bindToWebHandler(webHandler).build();
	}

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

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
