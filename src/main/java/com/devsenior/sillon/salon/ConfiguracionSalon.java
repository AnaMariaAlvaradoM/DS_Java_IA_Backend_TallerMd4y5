package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuracion de agenda del salon. No es una tabla aparte: sus columnas viven dentro de "salones" (@Embeddable).
 */
@Embeddable
public class ConfiguracionSalon {

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    @Column(name = "abre_domingos", nullable = false)
    private boolean abreDomingos;

    @Column(name = "intervalo_minutos", nullable = false)
    private int intervaloMinutos;

    /** JPA necesita un constructor vacio. */
    protected ConfiguracionSalon() {
    }

    public ConfiguracionSalon(LocalTime horaApertura, LocalTime horaCierre, boolean abreDomingos,
            int intervaloMinutos) {
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.abreDomingos = abreDomingos;
        this.intervaloMinutos = intervaloMinutos;
    }

    /** Horario con el que nace todo salon: 8:00 a 19:00, sin domingos, intervalo de 30 minutos. */
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
