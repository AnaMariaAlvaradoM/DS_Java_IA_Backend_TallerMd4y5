package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class ProveedorNoDisponibleException extends ExcepcionApi {
    public ProveedorNoDisponibleException(String mensaje) { super(HttpStatus.SERVICE_UNAVAILABLE, mensaje); }
}
