package com.devsenior.sillon.ia.proveedor;

import com.devsenior.sillon.ia.dto.RespuestaIa;

public interface ProveedorIa {
    String nombre();

    RespuestaIa generar(String instruccionesSistema, String mensajeUsuario);
}