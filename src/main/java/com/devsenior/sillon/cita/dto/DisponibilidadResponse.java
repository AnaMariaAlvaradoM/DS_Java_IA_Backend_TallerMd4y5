package com.devsenior.sillon.cita.dto;

import java.time.LocalDate;
import java.util.List;

/** horariosLibres va como texto "HH:mm:ss" (por ejemplo "10:00:00"), tal como lo espera la colección. */
public record DisponibilidadResponse(
        LocalDate fecha,
        String servicio,
        String estilista,
        int duracionMinutos,
        List<String> horariosLibres
) {
}
