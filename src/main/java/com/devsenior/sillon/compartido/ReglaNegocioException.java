package com.devsenior.sillon.compartido;

/** 409: la peticion choca con una regla de negocio (cruce, horario, transicion, correo repetido, plan ya activo). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
