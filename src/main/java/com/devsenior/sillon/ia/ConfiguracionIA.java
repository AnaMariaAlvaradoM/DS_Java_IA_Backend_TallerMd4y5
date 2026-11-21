
package com.devsenior.sillon.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        ConfiguracionIA.PropiedadesIA.class
)
public class ConfiguracionIA {

    @ConfigurationProperties(prefix = "sillon.ia")
    public record PropiedadesIA(
            ProveedorOpenAI openai,
            ProveedorClaude claude
    ) {
    }

    public record ProveedorOpenAI(
            String url,
            String llave,
            String modelo
    ) {
    }

    public record ProveedorClaude(
            String url,
            String llave,
            String modelo
    ) {
    }
}
