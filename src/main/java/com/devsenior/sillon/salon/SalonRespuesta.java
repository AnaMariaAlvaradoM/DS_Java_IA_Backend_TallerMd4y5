package com.devsenior.sillon.salon;

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
        ConfiguracionRespuesta configuracion,
        LocalDateTime fechaCreacion) {
}
