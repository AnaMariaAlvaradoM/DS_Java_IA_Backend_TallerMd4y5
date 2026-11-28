package com.devSenior.sillon.compartido.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@Configuration
@OpenAPIDefinition(
		info = @Info(title = "Sillón API", version = "1.0"),
		tags = {
				@Tag(name = "0. Público"),
				@Tag(name = "1. Autenticación"),
				@Tag(name = "2. Mi salón"),
				@Tag(name = "3. Servicios"),
				@Tag(name = "4. Estilistas"),
				@Tag(name = "5. Clientes"),
				@Tag(name = "6. Agenda"),
				@Tag(name = "7. Pagos"),
				@Tag(name = "8. Solicitudes"),
				@Tag(name = "9. Administración")
		},
		security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}