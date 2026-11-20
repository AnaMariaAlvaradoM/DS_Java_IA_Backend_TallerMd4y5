package com.devsenior.sillon.salon;

import jakarta.persistence.Embeddable;

import java.time.LocalTime;

@Embeddable
public class ConfiguracionSalon {
    private LocalTime horaApertura = LocalTime.of(8, 0);
    private LocalTime horaCierre = LocalTime.of(19, 0);
    private boolean abreDomingos;
    private int intervaloMinutos = 30;

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime valor) {
        this.horaApertura = valor;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime valor) {
        this.horaCierre = valor;
    }

    public boolean getAbreDomingos() {
        return abreDomingos;
    }

    public void setAbreDomingos(boolean valor) {
        this.abreDomingos = valor;
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(int valor) {
        this.intervaloMinutos = valor;
    }
}
