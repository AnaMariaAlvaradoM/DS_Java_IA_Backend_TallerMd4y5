package com.devsenior.sillon.publico;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Respuesta del calculo de disponibilidad de un estilista para un servicio en una fecha dada.
 */
public record DisponibilidadRespuesta(
        LocalDate fecha,
        String servicio,
        String estilista,
        int duracionMinutos,
        List<LocalTime> horariosLibres) {
}
