package com.devsenior.sillon.ia.excepcion;

import com.devsenior.sillon.compartido.excepcion.ExcepcionApi;
import org.springframework.http.HttpStatus;

/** 503: ningún proveedor de IA respondió. */
public class IaNoDisponibleException extends ExcepcionApi {

    public IaNoDisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "El asistente de IA no está disponible en este momento. Intente más tarde");
    }
}
