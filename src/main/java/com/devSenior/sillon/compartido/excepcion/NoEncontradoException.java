package com.devSenior.sillon.compartido.excepcion;

public class NoEncontradoException extends ExcepcionDeApi {

	public NoEncontradoException(String mensaje) {
		super(404, "Not Found", mensaje);
	}

}
