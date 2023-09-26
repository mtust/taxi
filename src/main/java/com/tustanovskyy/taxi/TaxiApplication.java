package com.tustanovskyy.taxi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
@Slf4j
public class TaxiApplication {

	public static void main(String[] args) {
		String port = System.getenv("PORT");
		if (port == null) {
			port = "8080";
			log.warn("$PORT environment variable not set, defaulting to 8080");
		}
		SpringApplication app = new SpringApplication(TaxiApplication.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", port));
		app.run(args);
	}
}
