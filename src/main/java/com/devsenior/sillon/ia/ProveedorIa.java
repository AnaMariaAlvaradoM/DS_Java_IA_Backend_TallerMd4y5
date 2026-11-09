package com.devsenior.sillon.ia;

public interface ProveedorIa {
    String nombre();
    RespuestaIa generar(PeticionIa peticion);

    default RespuestaIa generar(String prompt) {
        return generar(new PeticionIa(null, prompt));
    }
}
