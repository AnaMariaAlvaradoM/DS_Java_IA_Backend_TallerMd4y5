package com.devsenior.sillon.salon;

import org.springframework.stereotype.Component;

@Component
public class SalonMapper {
    public SalonResponse convertir(Salon salon) {
        ConfiguracionSalon config = salon.getConfiguracion();
        return new SalonResponse(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), salon.getPlan(), salon.getCreditosIa(),
                new ConfiguracionSalonResponse(config.getHoraApertura(), config.getHoraCierre(),
                        config.isAbreDomingos(), config.getIntervaloMinutos()), salon.getFechaCreacion());
    }
}
