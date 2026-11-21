package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ExcepcionApi {
    public AccesoDenegadoException(String mensaje) { super(HttpStatus.FORBIDDEN, mensaje); }
}
