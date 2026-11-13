package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Plan;

public record SalonResponse(
        Long id,
        String nombre,
        String slug,
        String telefono,
        String direccion,
        Plan plan,
        Integer creditosIa,
        ConfiguracionSalon configuracion
) {
}
