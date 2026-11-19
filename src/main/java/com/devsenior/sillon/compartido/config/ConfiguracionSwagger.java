package com.devsenior.sillon.compartido.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI en /swagger-ui.html con título "Sillón API" y esquema Bearer JWT para el botón Authorize.
 * Cada controlador protegido usa @SecurityRequirement(name = "bearerAuth").
 */
@Configuration
public class ConfiguracionSwagger {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI sillonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sillón API")
                        .version("1.0")
                        .description("Agenda inteligente para barberías y salones de belleza: SaaS multi-inquilino "
                                + "con pagos en Stripe (modo prueba) e IA con créditos. "
                                + "Para las rutas protegidas: registre o inicie sesión en 1 Autenticación, "
                                + "copie el token y péguelo en Authorize."))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
