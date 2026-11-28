package com.devSenior.sillon.compartido.excepcion;

public class ConflictoException extends ExcepcionDeApi {

	public ConflictoException(String mensaje) {
		super(409, "Conflict", mensaje);
	}

}
