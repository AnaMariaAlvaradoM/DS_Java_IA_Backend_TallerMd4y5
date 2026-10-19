package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class PagoRequeridoException extends ExcepcionSillon {
    public PagoRequeridoException(String mensaje) {
        super(HttpStatus.PAYMENT_REQUIRED, mensaje);
    }
}
