package com.devsenior.sillon.salon.modelo;

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

	public static ConfiguracionSalon predeterminada() {
		ConfiguracionSalon configuracion = new ConfiguracionSalon();
		configuracion.setHoraApertura(LocalTime.of(8, 0));
		configuracion.setHoraCierre(LocalTime.of(19, 0));
		configuracion.setAbreDomingos(false);
		configuracion.setIntervaloMinutos(30);
		return configuracion;
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
