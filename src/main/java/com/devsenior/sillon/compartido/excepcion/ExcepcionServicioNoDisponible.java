package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionServicioNoDisponible extends ExcepcionSillon {

	public ExcepcionServicioNoDisponible(String mensaje) {
		super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
	}

}
