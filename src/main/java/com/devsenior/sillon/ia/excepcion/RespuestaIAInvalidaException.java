package com.devsenior.sillon.ia.excepcion;

import com.devsenior.sillon.compartido.excepcion.ExcepcionApi;
import org.springframework.http.HttpStatus;

/** 502: la IA respondió algo que no se pudo usar (sin JSON, JSON inválido o con datos faltantes). */
public class RespuestaIAInvalidaException extends ExcepcionApi {

    public RespuestaIAInvalidaException(String detalle) {
        super(HttpStatus.BAD_GATEWAY, "La IA respondió algo que no se pudo usar: " + detalle);
    }
}
