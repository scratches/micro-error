/*
 * Copyright 2016-2017 the original author or authors.
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

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Dave Syer
 *
 */
public class MicroApplication {

	public static void main(String[] args) throws Exception {
		long t0 = System.currentTimeMillis();
		new MicroApplication().run(args);
		System.err.println(
				"Started HttpServer: " + (System.currentTimeMillis() - t0) + "ms");
	}

	public GenericApplicationContext run(String... args) {
		GenericApplicationContext context = new GenericApplicationContext();
		ConfigurableEnvironment environment = context.getEnvironment();
		environment.getPropertySources()
				.addFirst(new SimpleCommandLinePropertySource("commandLine", args));
		String logConfig = environment.resolvePlaceholders("${logging.config:}");
		LogFile logFile = LogFile.get(environment);
		LoggingSystem.get(getClass().getClassLoader()).initialize(
				new LoggingInitializationContext(environment), logConfig, logFile);
		context.registerBean(RouterFunction.class,
				() -> RouterFunctions
						.route(GET("/"),
								request -> ok().body(Mono.just("Hello"), String.class))
						.andRoute(GET("/boom"),
								request -> ok().body(
										Flux.<String>error(
												new IllegalStateException("Planned")),
										String.class))
						.andRoute(POST("/"),
								request -> ok().body(
										request.bodyToMono(String.class)
												.map(value -> value.toUpperCase()),
										String.class)));
		context.registerBean(DefaultErrorWebExceptionHandler.class,
				() -> errorHandler(context));
		context.registerBean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME,
				HttpWebHandlerAdapter.class, () -> httpHandler(context));
		context.addApplicationListener(new ServerListener(context));
		context.refresh();
		return context;
	}

	private HttpWebHandlerAdapter httpHandler(GenericApplicationContext context) {
		return (HttpWebHandlerAdapter) RouterFunctions.toHttpHandler(
				context.getBean(RouterFunction.class),
				HandlerStrategies.empty()
						.exceptionHandler(context.getBean(WebExceptionHandler.class))
						.codecs(config -> config.registerDefaults(true)).build());
	}

	private DefaultErrorWebExceptionHandler errorHandler(
			GenericApplicationContext context) {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes());
		context.registerBean(ErrorProperties.class, () -> new ErrorProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		DefaultErrorWebExceptionHandler handler = new DefaultErrorWebExceptionHandler(
				context.getBean(ErrorAttributes.class),
				context.getBean(ResourceProperties.class),
				context.getBean(ErrorProperties.class), context);
		ServerCodecConfigurer codecs = ServerCodecConfigurer.create();
		handler.setMessageWriters(codecs.getWriters());
		handler.setMessageReaders(codecs.getReaders());
		return handler;
	}

}