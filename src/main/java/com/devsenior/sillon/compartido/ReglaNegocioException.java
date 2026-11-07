package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class ReglaNegocioException extends ExcepcionApi {
    public ReglaNegocioException(String mensaje) { super(HttpStatus.CONFLICT, mensaje); }
}
