package com.devsenior.sillon.compartido.dto;

import java.time.LocalDateTime;

/** Formato único de error de toda la API, incluidos 401 y 403. */
public record ErrorRespuesta(
        int codigo,
        String error,
        String mensaje,
        LocalDateTime fecha
) {
}
