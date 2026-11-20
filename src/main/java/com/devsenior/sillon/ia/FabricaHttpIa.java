package com.devsenior.sillon.ia;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConfiguracionIa.class)
public class FabricaHttpIa {

    @Bean
    @Order(1)
    public ProveedorIa proveedorGemini(ConfiguracionIa configuracion, ObjectMapper mapper) {
        return new ProveedorHttpIa("Gemini", false, configuracion.openai(),
                cliente(configuracion.openai(), configuracion), mapper);
    }

    @Bean
    @Order(2)
    public ProveedorIa proveedorClaude(ConfiguracionIa configuracion, ObjectMapper mapper) {
        return new ProveedorHttpIa("Claude", true, configuracion.claude(),
                cliente(configuracion.claude(), configuracion), mapper);
    }

    private RestClient cliente(ConfiguracionIa.Proveedor proveedor, ConfiguracionIa configuracion) {
        if (configuracion.tiempoMaximo().isNegative() || configuracion.tiempoMaximo().isZero()) {
            throw new IllegalStateException("El tiempo maximo de IA debe ser positivo");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(configuracion.tiempoMaximo());
        factory.setReadTimeout(configuracion.tiempoMaximo());
        return RestClient.builder().baseUrl(proveedor.url()).requestFactory(factory).build();
    }
}
