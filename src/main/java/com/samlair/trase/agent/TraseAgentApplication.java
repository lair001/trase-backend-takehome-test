package com.samlair.trase.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the Trase agent API.
 */
@SpringBootApplication
@EnableScheduling
public class TraseAgentApplication {

	/**
	 * Bootstraps the Spring application.
	 *
	 * @param args command line arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(TraseAgentApplication.class, args);
	}
}
