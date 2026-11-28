package com.devSenior.sillon.compartido.excepcion;

public class PagoRequeridoException extends ExcepcionDeApi {

	public PagoRequeridoException(String mensaje) {
		super(402, "Payment Required", mensaje);
	}

}
