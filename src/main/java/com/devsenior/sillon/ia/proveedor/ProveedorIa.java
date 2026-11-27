
package com.devsenior.sillon.ia.proveedor;

import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;

public interface ProveedorIa {

    ResultadoProveedorIa generar(
            String instrucciones,
            String mensajeUsuario);
}
