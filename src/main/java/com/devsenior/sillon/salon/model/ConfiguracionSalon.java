package com.devsenior.sillon.salon.model;

import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.time.LocalTime;

//horaApertura, horaCierre, abreDomingos,
//intervaloMinutos
@Embeddable
public class ConfiguracionSalon {
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private boolean abreDomingos;
    private Integer intervaloMinutos;

    public ConfiguracionSalon(LocalTime horaApertura, LocalTime horaCierre, boolean abreDomingos, Integer intervaloMinutos) {
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.abreDomingos = abreDomingos;
        this.intervaloMinutos = intervaloMinutos;
    }

    public ConfiguracionSalon() {
    }

    public static ConfiguracionSalon porDefecto() {
        return new ConfiguracionSalon(
                LocalTime.of(8, 0),
                LocalTime.of(19, 0),
                false,
                30
        );
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(Integer intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }

    public boolean isAbreDomingos() {
        return abreDomingos;
    }

    public void setAbreDomingos(boolean abreDomingos) {
        this.abreDomingos = abreDomingos;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }
}