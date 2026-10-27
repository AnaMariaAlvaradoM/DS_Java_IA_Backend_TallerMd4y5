package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class PasarelaException extends ExcepcionSillon {
    public PasarelaException(String mensaje) {
        super(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
