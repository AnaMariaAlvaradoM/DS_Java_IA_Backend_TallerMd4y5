package com.devSenior.sillon.compartido.excepcion;

public class ServicioNoDisponibleException extends ExcepcionDeApi {

	public ServicioNoDisponibleException(String mensaje) {
		super(503, "Service Unavailable", mensaje);
	}

}
