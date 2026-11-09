package com.devsenior.sillon.ia;

public class FalloProveedorIa extends RuntimeException {
    public enum Tipo { CONFIGURACION, CONECTIVIDAD, AUTENTICACION, TIMEOUT, RESPUESTA_INVALIDA, TEMPORAL }
    private final Tipo tipo;
    private final Integer codigoHttp;

    public FalloProveedorIa(Tipo tipo) {
        this(tipo, null);
    }

    public FalloProveedorIa(Tipo tipo, Integer codigoHttp) {
        super("Proveedor IA no disponible: " + tipo);
        this.tipo = tipo;
        this.codigoHttp = codigoHttp;
    }

    public Tipo getTipo() { return tipo; }
    public Integer getCodigoHttp() { return codigoHttp; }
}
