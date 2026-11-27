package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionRecursoNoEncontrado extends ExcepcionSillon {

	public ExcepcionRecursoNoEncontrado(String mensaje) {
		super(HttpStatus.NOT_FOUND, mensaje);
	}

}
