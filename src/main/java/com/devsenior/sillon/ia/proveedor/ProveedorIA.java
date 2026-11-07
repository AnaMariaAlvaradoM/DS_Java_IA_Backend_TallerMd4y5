package com.devsenior.sillon.ia.proveedor;

import com.devsenior.sillon.ia.RespuestaIA;

public interface ProveedorIA {
    String nombre();

    RespuestaIA completar(String instrucciones, String mensaje);
}
