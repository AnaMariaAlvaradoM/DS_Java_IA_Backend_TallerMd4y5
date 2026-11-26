package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 502: Stripe o la IA respondieron algo que no se pudo usar. */
public class PasarelaException extends ApiException {

    public PasarelaException(String mensaje) {
        super(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
