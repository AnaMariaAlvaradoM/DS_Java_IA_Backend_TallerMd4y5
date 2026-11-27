
package com.devsenior.sillon.compartido.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "Bearer JWT";

    @Bean
    public OpenAPI openApiSillon() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sillón API")
                        .version("1.0.0")
                        .description(
                                "API REST de Sillón: gestión de salones, "
                                        + "servicios, estilistas, clientes, "
                                        + "agenda, pagos e inteligencia artificial."))
                .components(new Components()
                        .addSecuritySchemes(
                                ESQUEMA_JWT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Ingrese el token JWT obtenido "
                                                        + "al iniciar sesión.")));
    }
}
