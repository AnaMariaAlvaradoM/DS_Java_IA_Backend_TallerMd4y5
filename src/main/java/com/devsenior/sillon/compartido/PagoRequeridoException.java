package com.devsenior.sillon.compartido;
import org.springframework.http.HttpStatus;
public class PagoRequeridoException extends ExcepcionApi {
    public PagoRequeridoException(String mensaje) { super(HttpStatus.PAYMENT_REQUIRED, mensaje); }
}
