
package com.devsenior.sillon.salon;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record RespuestaSalon(
        Long id,
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        Plan plan,
        int creditosIa,
        ConfiguracionRespuesta configuracion,
        LocalDateTime fechaCreacion
) {

    public static RespuestaSalon desde(Salon salon) {

        ConfiguracionSalon config = salon.getConfiguracion();

        ConfiguracionRespuesta configuracionRespuesta =
                new ConfiguracionRespuesta(
                        config.getHoraApertura(),
                        config.getHoraCierre(),
                        config.isAbreDomingos(),
                        config.getIntervaloMinutos()
                );

        return new RespuestaSalon(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                configuracionRespuesta,
                salon.getFechaCreacion()
        );
    }

    public record ConfiguracionRespuesta(
            LocalTime horaApertura,
            LocalTime horaCierre,
            boolean abreDomingos,
            int intervaloMinutos
    ) {
    }
}
