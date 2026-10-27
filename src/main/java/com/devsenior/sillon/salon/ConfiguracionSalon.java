package com.devsenior.sillon.salon;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

@Embeddable
public class ConfiguracionSalon {
    @Column(nullable = false)
    private LocalTime horaApertura;
    @Column(nullable = false)
    private LocalTime horaCierre;
    @Column(nullable = false)
    private boolean abreDomingos;
    @Column(nullable = false)
    private int intervaloMinutos;

    public ConfiguracionSalon() {
    }

    public static ConfiguracionSalon porDefecto() {
        ConfiguracionSalon configuracion = new ConfiguracionSalon();
        configuracion.setHoraApertura(LocalTime.of(8, 0));
        configuracion.setHoraCierre(LocalTime.of(19, 0));
        configuracion.setAbreDomingos(false);
        configuracion.setIntervaloMinutos(30);
        return configuracion;
    }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
    public boolean isAbreDomingos() { return abreDomingos; }
    public void setAbreDomingos(boolean abreDomingos) { this.abreDomingos = abreDomingos; }
    public int getIntervaloMinutos() { return intervaloMinutos; }
    public void setIntervaloMinutos(int intervaloMinutos) { this.intervaloMinutos = intervaloMinutos; }
}
