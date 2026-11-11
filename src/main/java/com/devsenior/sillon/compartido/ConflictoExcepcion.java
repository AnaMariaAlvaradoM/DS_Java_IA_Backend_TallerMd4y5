package com.devsenior.sillon.compartido;

public class ConflictoExcepcion extends RuntimeException {
    public ConflictoExcepcion(String mensaje) {
        super(mensaje);
    }
}
