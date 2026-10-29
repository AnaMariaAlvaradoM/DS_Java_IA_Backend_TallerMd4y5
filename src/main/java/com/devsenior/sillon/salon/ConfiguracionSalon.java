package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.persistence.Embeddable;

@Embeddable
public class ConfiguracionSalon {

    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private boolean abreDomingos;
    private int intervaloMinutos;

    public ConfiguracionSalon() {
    }

    public ConfiguracionSalon(LocalTime horaApertura, LocalTime horaCierre, boolean abreDomingos,
            int intervaloMinutos) {
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.abreDomingos = abreDomingos;
        this.intervaloMinutos = intervaloMinutos;
    }

    public static ConfiguracionSalon porDefecto() {
        return new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), false, 30);
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    public boolean isAbreDomingos() {
        return abreDomingos;
    }

    public void setAbreDomingos(boolean abreDomingos) {
        this.abreDomingos = abreDomingos;
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(int intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }
}
