package com.devsenior.sillon.compartido;

/** 402: limite del plan GRATIS o salon sin creditos de IA. */
public class PagoRequeridoException extends RuntimeException {

    public PagoRequeridoException(String mensaje) {
        super(mensaje);
    }
}
