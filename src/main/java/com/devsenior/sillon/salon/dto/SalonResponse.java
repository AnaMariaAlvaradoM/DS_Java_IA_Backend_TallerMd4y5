package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
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
        ConfiguracionSalonDto configuracion,
        LocalDateTime fechaCreacion) {
    public static SalonResponse desde(Salon salon) {
        return new SalonResponse(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                ConfiguracionSalonDto.desde(salon.getConfiguracion()),
                salon.getFechaCreacion());
    }
}