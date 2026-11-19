package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionConflicto extends ExcepcionSillon {

	public ExcepcionConflicto(String mensaje) {
		super(HttpStatus.CONFLICT, mensaje);
	}

}
