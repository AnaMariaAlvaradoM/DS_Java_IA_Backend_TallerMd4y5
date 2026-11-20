package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.Plan;

import java.time.LocalDateTime;

public record SalonRespuesta(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        Plan plan,
        int creditosIa,
        ConfiguracionSalonDTO configuracion,
        LocalDateTime fechaCreacion
) {
}
