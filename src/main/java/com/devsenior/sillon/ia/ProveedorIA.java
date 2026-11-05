package com.devsenior.sillon.ia;

public interface ProveedorIA {

    String nombre();

    RespuestaIA completar(String instrucciones, String mensaje);
}
