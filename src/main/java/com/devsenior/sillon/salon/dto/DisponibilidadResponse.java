package com.devsenior.sillon.salon.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadResponse(
        LocalDate fecha,
        String servicio,
        String estilista,
        int duracionMinutos,
        List<LocalTime> horariosLibres) {
}