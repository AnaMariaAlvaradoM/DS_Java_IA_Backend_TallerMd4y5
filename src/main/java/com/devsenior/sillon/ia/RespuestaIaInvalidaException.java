package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;

public class RespuestaIaInvalidaException extends ErrorDePasarelaException {

    public RespuestaIaInvalidaException() {
        super("La respuesta de la IA no contiene un mensaje válido de máximo 700 caracteres");
    }
}
