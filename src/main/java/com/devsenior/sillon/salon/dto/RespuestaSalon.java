package com.devsenior.sillon.salon.dto;

import java.time.LocalDateTime;

import com.devsenior.sillon.salon.modelo.Plan;

public record RespuestaSalon(

        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        Plan plan,
        int creditosIa,
        ConfiguracionSalonDto configuracion,
        LocalDateTime fechaCreacion

) {
}