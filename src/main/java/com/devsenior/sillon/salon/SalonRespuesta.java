package com.devsenior.sillon.salon;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

    public record ConfiguracionRespuesta(
            LocalTime horaApertura,
            LocalTime horaCierre,
            boolean abreDomingos,
            int intervaloMinutos) {
    }

    public static SalonRespuesta de(Salon salon) {
        ConfiguracionSalon c = salon.getConfiguracion();
        return new SalonRespuesta(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                new ConfiguracionRespuesta(
                        c.getHoraApertura(),
                        c.getHoraCierre(),
                        c.isAbreDomingos(),
                        c.getIntervaloMinutos()),
                salon.getFechaCreacion());
    }
}
