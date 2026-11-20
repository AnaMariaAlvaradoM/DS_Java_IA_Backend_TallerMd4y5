package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "sillon.ia")
public record ConfiguracionIa(@NotNull @Valid Proveedor openai, @NotNull @Valid Proveedor claude,
        @NotNull Duration tiempoMaximo) {

    public record Proveedor(@NotBlank String url, @NotBlank String modelo, String llave,
            @NotNull @DecimalMin("0") BigDecimal costoEntradaMillon,
            @NotNull @DecimalMin("0") BigDecimal costoSalidaMillon) {

        @Override
        public String toString() {
            return "Proveedor[credenciales protegidas]";
        }
    }

    @Override
    public String toString() {
        return "ConfiguracionIa[credenciales protegidas]";
    }
}
