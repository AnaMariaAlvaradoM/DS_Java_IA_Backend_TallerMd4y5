package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class DatosInvalidosException extends ExcepcionSillon {
    public DatosInvalidosException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
