package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 402: límite del plan GRATIS o salón sin créditos de IA. */
public class PagoRequeridoException extends ExcepcionApi {

    public PagoRequeridoException(String mensaje) {
        super(HttpStatus.PAYMENT_REQUIRED, mensaje);
    }
}
