package com.devsenior.sillon.salon.mapper;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.salon.dto.ConfiguracionSalonDto;
import com.devsenior.sillon.salon.dto.RespuestaSalon;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Salon;

@Component
public class SalonMapper {

    public RespuestaSalon aRespuesta(Salon salon) {

        return new RespuestaSalon(
                salon.getId(),
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                salon.getPlan(),
                salon.getCreditosIa(),
                aConfiguracionDto(salon.getConfiguracion()),
                salon.getFechaCreacion());
    }

    public ConfiguracionSalonDto aConfiguracionDto(
            ConfiguracionSalon configuracion) {

        return new ConfiguracionSalonDto(
                configuracion.getHoraApertura(),
                configuracion.getHoraCierre(),
                configuracion.isAbreDomingos(),
                configuracion.getIntervaloMinutos());
    }

    public ConfiguracionSalon aEntidad(
            ConfiguracionSalonDto dto) {

        ConfiguracionSalon configuracion = new ConfiguracionSalon();

        configuracion.setHoraApertura(dto.horaApertura());
        configuracion.setHoraCierre(dto.horaCierre());
        configuracion.setAbreDomingos(dto.abreDomingos());
        configuracion.setIntervaloMinutos(dto.intervaloMinutos());

        return configuracion;
    }

}