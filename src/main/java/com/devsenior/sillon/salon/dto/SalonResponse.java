package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.model.Plan;
import java.time.LocalDateTime;

public record SalonResponse(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        Plan plan,
        int creditosIa,
        ConfiguracionResponse configuracion,
        LocalDateTime fechaCreacion
) {
}
