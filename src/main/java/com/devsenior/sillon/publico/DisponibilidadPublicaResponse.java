
package com.devsenior.sillon.publico;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadPublicaResponse(
        LocalDate fecha,
        String servicio,
        String estilista,
        int duracionMinutos,
        List<LocalTime> horariosLibres
) {
}
