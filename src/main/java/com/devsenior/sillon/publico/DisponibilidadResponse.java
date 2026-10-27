package com.devsenior.sillon.publico;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadResponse(LocalDate fecha, String servicio, String estilista,
        int duracionMinutos, List<String> horariosLibres) { }
