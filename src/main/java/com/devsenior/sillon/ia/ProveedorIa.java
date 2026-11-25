package com.devsenior.sillon.ia;

public interface ProveedorIa {

    String nombre();

    RespuestaIa completar(String instrucciones, String mensaje);
}
