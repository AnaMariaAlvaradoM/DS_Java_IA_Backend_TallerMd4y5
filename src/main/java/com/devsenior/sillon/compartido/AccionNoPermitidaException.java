package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class AccionNoPermitidaException extends ExcepcionApi {
    public AccionNoPermitidaException(String mensaje) { super(HttpStatus.FORBIDDEN, mensaje); }
}
