package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionNoPermitida extends ExcepcionSillon {

	public ExcepcionNoPermitida(String mensaje) {
		super(HttpStatus.FORBIDDEN, mensaje);
	}

}
