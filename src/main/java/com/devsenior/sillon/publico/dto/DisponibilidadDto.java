package com.devsenior.sillon.publico.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadDto (
        LocalDate fecha,
        String servicio,
        String estilista,
        Integer duracionMinutos,
        List<String> horariosLibres
) {}
