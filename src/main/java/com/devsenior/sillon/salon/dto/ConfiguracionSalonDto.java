package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import java.time.LocalTime;

public record ConfiguracionSalonDto(
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean abreDomingos,
        int intervaloMinutos) {
    public static ConfiguracionSalonDto desde(ConfiguracionSalon config) {
        return new ConfiguracionSalonDto(
                config.getHoraApertura(),
                config.getHoraCierre(),
                config.isAbreDomingos(),
                config.getIntervaloMinutos());
    }
}