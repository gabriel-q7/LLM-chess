package com.example.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChessApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ChessApplication.class);
		application.addListeners(new DatabaseDirectoryInitializer());
		application.run(args);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
