package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class FirmaInvalidaException extends ExcepcionApi {
    public FirmaInvalidaException(String mensaje) { super(HttpStatus.BAD_REQUEST, mensaje); }
}
