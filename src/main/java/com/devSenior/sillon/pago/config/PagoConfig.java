package com.devSenior.sillon.pago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

@Configuration
public class PagoConfig {

	@Bean
	public StripeClient stripeClient(StripePropiedades propiedades) {
		return StripeClient.builder()
				.setApiKey(propiedades.getClaveSecreta())
				.setApiBase(propiedades.getUrlApi())
				.build();
	}

}
