package com.devsenior.sillon.salon;

import java.time.LocalTime;
import jakarta.persistence.Embeddable;

@Embeddable
public class ConfiguracionSalon {
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private Boolean abreDomingos;
    private Integer intervaloMinutos;

    public static ConfiguracionSalon porDefecto() {
        ConfiguracionSalon configuracion = new ConfiguracionSalon();
        configuracion.horaApertura = LocalTime.of(8, 0);
        configuracion.horaCierre = LocalTime.of(19, 0);
        configuracion.abreDomingos = false;
        configuracion.intervaloMinutos = 30;
        return configuracion;
    }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
    public Boolean getAbreDomingos() { return abreDomingos; }
    public void setAbreDomingos(Boolean abreDomingos) { this.abreDomingos = abreDomingos; }
    public Integer getIntervaloMinutos() { return intervaloMinutos; }
    public void setIntervaloMinutos(Integer intervaloMinutos) { this.intervaloMinutos = intervaloMinutos; }
}
