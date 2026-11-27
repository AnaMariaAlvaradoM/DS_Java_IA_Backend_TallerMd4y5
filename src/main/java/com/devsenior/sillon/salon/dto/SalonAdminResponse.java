package com.devsenior.sillon.salon.dto;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;

import java.time.LocalDate;

public record SalonAdminResponse(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String telefono,
        Plan plan,
        int creditosIa,
        LocalDate fechaCreacion
) {
    public static SalonAdminResponse desdeEntidad(Salon salon) {
        return new SalonAdminResponse(
                salon.getId(),
                salon.getNombresalon(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                salon.getFechaCreacion()
        );
    }


























}
