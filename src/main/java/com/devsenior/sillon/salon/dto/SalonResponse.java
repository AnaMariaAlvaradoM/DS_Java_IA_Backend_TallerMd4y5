package com.devsenior.sillon.salon.dto;

import java.time.LocalDateTime;

/** Respuesta de GET /api/salon y PUT /api/salon/configuracion (SPEC.md §7.6). */
public record SalonResponse(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String plan,
        int creditosIa,
        ConfiguracionResponse configuracion,
        LocalDateTime fechaCreacion) {
}
