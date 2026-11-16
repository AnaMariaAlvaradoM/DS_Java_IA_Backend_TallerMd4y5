package com.devSenior.sillon.compartido.excepcion;

public class IntegracionInvalidaException extends ExcepcionDeApi {

	public IntegracionInvalidaException(String mensaje) {
		super(502, "Bad Gateway", mensaje);
	}

}
