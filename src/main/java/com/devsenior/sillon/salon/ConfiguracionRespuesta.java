package com.devsenior.sillon.salon;

import java.time.LocalTime;

public record ConfiguracionRespuesta(LocalTime horaApertura, LocalTime horaCierre,
        Boolean abreDomingos, Integer intervaloMinutos) {
    public static ConfiguracionRespuesta desde(ConfiguracionSalon configuracion) {
        return new ConfiguracionRespuesta(configuracion.getHoraApertura(), configuracion.getHoraCierre(),
                configuracion.getAbreDomingos(), configuracion.getIntervaloMinutos());
    }
}
