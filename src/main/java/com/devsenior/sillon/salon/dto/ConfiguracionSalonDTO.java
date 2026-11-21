package com.devsenior.sillon.salon.dto;

import java.time.LocalTime;

public record ConfiguracionSalonDTO(
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean abreDomingos,
        int intervaloMinutos
) {
}
