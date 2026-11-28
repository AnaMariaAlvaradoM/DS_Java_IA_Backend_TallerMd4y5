package com.devSenior.sillon.compartido.excepcion;

public class FirmaInvalidaException extends ExcepcionDeApi {

	public FirmaInvalidaException(String mensaje) {
		super(400, "Bad Request", mensaje);
	}

}
