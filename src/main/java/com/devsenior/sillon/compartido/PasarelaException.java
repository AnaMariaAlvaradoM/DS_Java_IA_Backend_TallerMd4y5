package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class PasarelaException extends ExcepcionApi {
    public PasarelaException(String mensaje) { super(HttpStatus.BAD_GATEWAY, mensaje); }
}
