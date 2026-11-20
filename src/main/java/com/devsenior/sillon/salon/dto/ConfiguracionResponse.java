package com.devsenior.sillon.salon.dto;

import java.time.LocalTime;

/** Configuración del salón tal como se expone en la API (SPEC.md §7.6). */
public record ConfiguracionResponse(
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean abreDomingos,
        int intervaloMinutos) {
}
