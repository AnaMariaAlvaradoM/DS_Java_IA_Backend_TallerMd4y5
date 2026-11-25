package com.devsenior.sillon.salon;

import org.springframework.stereotype.Component;

/** Convierte la entidad en DTO. Se llama dentro del metodo transaccional del servicio. */
@Component
public class SalonMapper {

    public SalonRespuesta aRespuesta(Salon salon) {
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
                new ConfiguracionRespuesta(c.getHoraApertura(), c.getHoraCierre(), c.isAbreDomingos(),
                        c.getIntervaloMinutos()),
                salon.getFechaCreacion());
    }
}
