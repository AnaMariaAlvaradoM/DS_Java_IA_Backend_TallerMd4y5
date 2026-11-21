package com.devsenior.sillon.ia;

import com.devsenior.sillon.ia.dto.RespuestaIa;

public interface ProveedorIa {
    String getNombre();
    RespuestaIa generar(String promptSistema, String promptUsuario);
}
