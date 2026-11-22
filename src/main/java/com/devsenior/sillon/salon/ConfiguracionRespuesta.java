package com.devsenior.sillon.salon;

import java.time.LocalTime;

public record ConfiguracionRespuesta(
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean abreDomingos,
        int intervaloMinutos) {
}
