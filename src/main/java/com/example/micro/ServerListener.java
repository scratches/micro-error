/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.micro;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.ClassUtils;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * @author Dave Syer
 *
 */
public class ServerListener implements SmartApplicationListener {

	private static Log logger = LogFactory.getLog(ServerListener.class);

	private GenericApplicationContext context;

	public ServerListener(GenericApplicationContext context) {
		this.context = context;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		ApplicationContext context = ((ContextRefreshedEvent) event)
				.getApplicationContext();
		if (context != this.context) {
			return;
		}
		if (!ClassUtils.isPresent("org.springframework.http.server.reactive.HttpHandler",
				null)) {
			logger.info("No web server classes found so no server to start");
			return;
		}
		Integer port = Integer.valueOf(context.getEnvironment()
				.resolvePlaceholders("${server.port:${PORT:8080}}"));
		if (port >= 0) {
			HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context)
					.build();
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
			HttpServer httpServer = HttpServer.create().host("localhost").port(port)
					.handle(adapter);
			Thread thread = new Thread(() -> httpServer
					.bindUntilJavaShutdown(Duration.ofSeconds(60), this::callback),
					"server-startup");
			thread.setDaemon(false);
			thread.start();
		}
	}

	private void callback(DisposableServer server) {
		logger.info("Server started");
		try {
			double uptime = ManagementFactory.getRuntimeMXBean().getUptime();
			System.err.println("JVM running for " + uptime + "ms");
		}
		catch (Throwable e) {
		}
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return eventType.isAssignableFrom(ContextRefreshedEvent.class);
	}

}
