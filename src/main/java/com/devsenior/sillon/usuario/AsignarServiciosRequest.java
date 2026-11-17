package com.devsenior.sillon.usuario;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AsignarServiciosRequest(
        @NotEmpty(message = "Debe enviar al menos un ID de servicio")
        Set<Long> serviciosIds
) {
}