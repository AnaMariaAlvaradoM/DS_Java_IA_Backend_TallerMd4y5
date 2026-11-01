package com.devsenior.sillon.publico.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadResponse(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fecha,

        Long servicio,

        Long estilista,

        Integer duracionMinutos,

        @JsonFormat(pattern = "HH:mm")
        List<LocalTime> horariosLibres) {
}
