package com.devsenior.sillon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SillonApplication {

	public static void main(String[] args) {

		SpringApplication.run(SillonApplication.class, args);
	}

}
