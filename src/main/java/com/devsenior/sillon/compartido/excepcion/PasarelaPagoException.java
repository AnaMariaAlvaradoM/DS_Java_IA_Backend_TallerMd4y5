package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 502: Stripe respondió algo que no se pudo usar. */
public class PasarelaPagoException extends ExcepcionApi {

    public PasarelaPagoException(String mensaje) {
        super(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
