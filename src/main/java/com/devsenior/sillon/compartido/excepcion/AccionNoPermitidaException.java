package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class AccionNoPermitidaException extends ExcepcionSillon {
    public AccionNoPermitidaException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
