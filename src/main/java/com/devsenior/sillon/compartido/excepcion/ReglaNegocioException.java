package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends ExcepcionSillon {
    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
