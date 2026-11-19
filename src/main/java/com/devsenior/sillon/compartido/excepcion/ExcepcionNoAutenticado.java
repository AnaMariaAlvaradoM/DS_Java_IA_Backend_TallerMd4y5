package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionNoAutenticado extends ExcepcionSillon {

	public ExcepcionNoAutenticado(String mensaje) {
		super(HttpStatus.UNAUTHORIZED, mensaje);
	}

}
