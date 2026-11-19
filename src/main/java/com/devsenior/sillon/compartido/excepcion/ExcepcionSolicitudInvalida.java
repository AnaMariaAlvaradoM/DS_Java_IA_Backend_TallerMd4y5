package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionSolicitudInvalida extends ExcepcionSillon {

	public ExcepcionSolicitudInvalida(String mensaje) {
		super(HttpStatus.BAD_REQUEST, mensaje);
	}

}
