package com.devsenior.sillon.ia;

/** Un proveedor de IA que se llama por HTTP (sin SDK). */
public interface ProveedorIa {

    String nombre();

    /**
     * @param instrucciones  el prompt del sistema (reglas fijas)
     * @param mensajeUsuario el contenido variable (por ejemplo, la conversación del cliente)
     */
    RespuestaIa generar(String instrucciones, String mensajeUsuario);
}
