
package com.devsenior.sillon.pago;

import com.stripe.StripeClient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        ConfiguracionStripe.PropiedadesStripe.class
)
public class ConfiguracionStripe {

    @ConfigurationProperties(prefix = "sillon.stripe")
    public record PropiedadesStripe(
            String claveSecreta,
            String secretoWebhook,
            String precioPro,
            String precioCreditos,
            String urlApi
    ) {
    }

    @Bean
    public StripeClient stripeClient(
            PropiedadesStripe propiedades
    ) {
        return StripeClient.builder()
                .setApiKey(propiedades.claveSecreta())
                .setApiBase(propiedades.urlApi())
                .build();
    }
}
