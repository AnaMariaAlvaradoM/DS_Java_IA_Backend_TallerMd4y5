package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 402: límite del plan GRATIS o salón sin créditos de IA. */
public class PagoRequeridoException extends ApiException {

    public PagoRequeridoException(String mensaje) {
        super(HttpStatus.PAYMENT_REQUIRED, mensaje);
    }
}
