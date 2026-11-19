package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionIntegracion extends ExcepcionSillon {

	public ExcepcionIntegracion(String mensaje) {
		super(HttpStatus.BAD_GATEWAY, mensaje);
	}

}
