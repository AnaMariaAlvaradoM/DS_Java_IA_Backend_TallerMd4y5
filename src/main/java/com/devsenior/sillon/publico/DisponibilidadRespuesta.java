package com.devsenior.sillon.publico;

import java.time.LocalDate;
import java.util.List;

/** horariosLibres va como texto "HH:mm:ss" (por ejemplo "10:00:00"). */
public record DisponibilidadRespuesta(LocalDate fecha, String servicio, String estilista, int duracionMinutos,
        List<String> horariosLibres) {
}
