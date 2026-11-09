package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class CredencialesInvalidasException extends ExcepcionApi {
    public CredencialesInvalidasException(String mensaje) { super(HttpStatus.UNAUTHORIZED, mensaje); }
}
