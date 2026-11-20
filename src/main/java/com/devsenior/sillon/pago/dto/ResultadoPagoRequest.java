package com.devsenior.sillon.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResultadoPagoRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "exito|cancelado", message = "debe ser exito o cancelado")
        String estado) {
}
