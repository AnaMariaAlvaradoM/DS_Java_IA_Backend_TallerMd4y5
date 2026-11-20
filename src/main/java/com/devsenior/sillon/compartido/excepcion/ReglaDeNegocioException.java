package com.devsenior.sillon.compartido.excepcion;

/** 409: la petición choca con una regla de negocio. */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
