package com.devsenior.sillon.salon.mapper;

import com.devsenior.sillon.salon.dto.ConfiguracionResponse;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import org.springframework.stereotype.Component;

@Component
public class SalonMapper {

    public SalonResponse toResponse(Salon salon) {
        ConfiguracionSalon c = salon.getConfiguracion();
        ConfiguracionResponse configuracion = new ConfiguracionResponse(
                c.getHoraApertura(), c.getHoraCierre(), c.isAbreDomingos(), c.getIntervaloMinutos());
        return new SalonResponse(salon.getId(), salon.getNombre(), salon.getSlug(), salon.getCiudad(),
                salon.getDireccion(), salon.getTelefono(), salon.getPlan(), salon.getCreditosIa(),
                configuracion, salon.getFechaCreacion());
    }
}
