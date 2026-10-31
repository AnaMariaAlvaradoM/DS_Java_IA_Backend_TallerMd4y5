package com.devsenior.sillon.admin;

import com.devsenior.sillon.salon.Salon;

public class AdminMapper {

    private AdminMapper() {
    }

    public static AdminSalonRespuesta aRespuesta(Salon salon) {
        return new AdminSalonRespuesta(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getPlan(),
                salon.getCreditosIa(),
                salon.getFechaCreacion());
    }
}
