package com.devsenior.sillon.compartido.excepcion;

/** 402: límite del plan GRATIS o salón sin créditos de IA. */
public class PagoRequeridoException extends RuntimeException {

    public PagoRequeridoException(String mensaje) {
        super(mensaje);
    }
}
