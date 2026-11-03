package com.devsenior.sillon.compartido;

public class SolicitudInvalidaExcepcion extends RuntimeException {
    public SolicitudInvalidaExcepcion(String mensaje) {
        super(mensaje);
    }
}
