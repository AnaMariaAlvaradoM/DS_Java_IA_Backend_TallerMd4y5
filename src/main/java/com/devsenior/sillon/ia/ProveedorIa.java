package com.devsenior.sillon.ia;

public interface ProveedorIa {
    String nombre();

    RespuestaIa responder(String instrucciones);

    RespuestaIa responder(String instrucciones, String mensaje);
}
