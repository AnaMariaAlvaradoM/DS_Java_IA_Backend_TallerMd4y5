package com.devSenior.sillon.compartido.excepcion;

public class ProhibidoException extends ExcepcionDeApi {

	public ProhibidoException(String mensaje) {
		super(403, "Forbidden", mensaje);
	}

}
