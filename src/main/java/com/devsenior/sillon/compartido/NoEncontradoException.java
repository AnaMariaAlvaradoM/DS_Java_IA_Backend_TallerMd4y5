package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class NoEncontradoException extends ExcepcionApi {
    public NoEncontradoException(String mensaje) { super(HttpStatus.NOT_FOUND, mensaje); }
}
