
package com.devsenior.sillon.compartido;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionOpenApi {

    @Bean
    public OpenAPI configurarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sillón API")
                        .description(
                                "API REST para la gestión de barberías y salones de belleza. "
                                + "Permite administrar salones, servicios, estilistas, "
                                + "clientes, citas, pagos y funciones de inteligencia artificial."
                        )
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Autenticación mediante token JWT obtenido al iniciar sesión."
                                        )
                        )
                );
    }

    @Bean
    public OpenApiCustomizer configurarSeguridadEndpoints() {
        return openApi -> openApi.getPaths().forEach(
                (ruta, pathItem) -> {

                    boolean esPublica =
                            ruta.startsWith("/api/auth/")
                            || ruta.startsWith("/api/publico/")
                            || ruta.equals("/api/pagos/webhook")
                            || ruta.equals("/api/pagos/resultado");

                    if (!esPublica) {
                        pathItem.readOperations().forEach(
                                operacion -> operacion.addSecurityItem(
                                        new SecurityRequirement()
                                                .addList("bearerAuth")
                                )
                        );
                    }
                }
        );
    }
}
