package com.devSenior.sillon.compartido.excepcion;

public abstract class ExcepcionDeApi extends RuntimeException {

	private final int codigo;
	private final String error;

	protected ExcepcionDeApi(int codigo, String error, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
		this.error = error;
	}

	public int getCodigo() {
		return codigo;
	}

	public String getError() {
		return error;
	}

}
