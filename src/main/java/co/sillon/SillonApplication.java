package co.sillon;

import java.time.Clock;
import java.time.ZoneId;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SillonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SillonApplication.class, args);
    }

    @Bean
    Clock reloj(@Value("${sillon.zona}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    RestClient http() {
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(4)).withReadTimeout(Duration.ofSeconds(18));
        return RestClient.builder().requestFactory(
                org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder.simple().build(settings)).build();
    }
}
