package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;

import java.time.LocalDateTime;

public record SalonResponseDto(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        Plan plan,
        int creditosIa,
        ConfiguracionSalon configuracion,
        LocalDateTime fechaCreacion
) {}