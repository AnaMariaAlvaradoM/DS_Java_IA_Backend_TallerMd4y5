package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionPagoRequerido extends ExcepcionSillon {

	public ExcepcionPagoRequerido(String mensaje) {
		super(HttpStatus.PAYMENT_REQUIRED, mensaje);
	}

}
