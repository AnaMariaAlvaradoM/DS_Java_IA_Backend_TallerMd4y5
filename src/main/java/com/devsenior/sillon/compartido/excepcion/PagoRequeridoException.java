package com.devsenior.sillon.compartido.excepcion;

public class PagoRequeridoException extends RuntimeException {
    public PagoRequeridoException(String mensaje) {
        super(mensaje);
    }
}
