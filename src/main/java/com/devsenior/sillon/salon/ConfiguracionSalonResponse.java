package com.devsenior.sillon.salon;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record ConfiguracionSalonResponse(
        @JsonFormat(pattern = "HH:mm:ss") LocalTime horaApertura,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime horaCierre,
        boolean abreDomingos, int intervaloMinutos) {
}
