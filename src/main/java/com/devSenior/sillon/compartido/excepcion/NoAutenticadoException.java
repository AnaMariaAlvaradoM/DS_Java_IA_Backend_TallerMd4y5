package com.devSenior.sillon.compartido.excepcion;

public class NoAutenticadoException extends ExcepcionDeApi {

	public NoAutenticadoException(String mensaje) {
		super(401, "Unauthorized", mensaje);
	}

}
