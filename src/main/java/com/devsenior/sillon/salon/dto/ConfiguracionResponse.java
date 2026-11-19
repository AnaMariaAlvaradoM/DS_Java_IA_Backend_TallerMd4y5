package com.devsenior.sillon.salon.dto;

import java.time.LocalTime;

public record ConfiguracionResponse(
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean abreDomingos,
        int intervaloMinutos
) {
}
