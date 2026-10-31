package com.devsenior.sillon.admin;

import java.time.LocalDateTime;

import com.devsenior.sillon.salon.Plan;

public record AdminSalonRespuesta(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        Plan plan,
        int creditosIa,
        LocalDateTime fechaCreacion) {
}
