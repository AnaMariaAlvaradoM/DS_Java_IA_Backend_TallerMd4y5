package com.devsenior.sillon.salon.mapper;

import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.model.Salon;

public class SalonMapper {
    public static SalonResponse toResponse(Salon salon) {
        return new SalonResponse(
                salon.getId(),
                salon.getNombresalon(),
                salon.getSlug(),
                salon.getTelefono(),
                salon.getDireccion(),
                salon.getPlan(),
                salon.getCreditosIa(),
                salon.getConfiguracion()
        );
    }
}
