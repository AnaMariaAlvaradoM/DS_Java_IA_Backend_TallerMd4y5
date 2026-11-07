package com.devsenior.sillon;

import com.devsenior.sillon.config.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StripeProperties.class)
public class 	SillonApplication {

	public static void main(String[] args) {
		SpringApplication.run(SillonApplication.class, args);
	}

}
