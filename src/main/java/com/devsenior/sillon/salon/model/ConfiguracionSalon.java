package com.devsenior.sillon.salon.model;

import com.devsenior.sillon.compartido.util.FormatoColombia;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

/** Horario y agenda del salón. Vive dentro de la tabla salones (@Embeddable), como las preferencias de CampusFlow. */
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

    public ConfiguracionSalon(LocalTime horaApertura, LocalTime horaCierre, boolean abreDomingos, int intervaloMinutos) {
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.abreDomingos = abreDomingos;
        this.intervaloMinutos = intervaloMinutos;
    }

    /** Horario por defecto del registro: 8:00 a 19:00, sin domingos, intervalo de 30 minutos. */
    public static ConfiguracionSalon porDefecto() {
        return new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), false, 30);
    }

    /** "Lunes a sábado (domingos cerrado), de 8:00 a. m. a 7:00 p. m." */
    public String descripcion() {
        String dias = abreDomingos ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + FormatoColombia.hora(horaApertura) + " a " + FormatoColombia.hora(horaCierre);
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
