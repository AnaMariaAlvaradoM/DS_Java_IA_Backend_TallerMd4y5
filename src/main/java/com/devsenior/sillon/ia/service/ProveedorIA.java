package com.devsenior.sillon.ia.service;

import com.devsenior.sillon.ia.model.RespuestaIA;

/**
 * El "enchufe" común: el negocio conoce esta interfaz, no a Gemini ni a Claude.
 * Si falla, lanza ProveedorIAException indicando si es de configuración o temporal.
 */
public interface ProveedorIA {

    String nombre();

    RespuestaIA completar(String instrucciones, String mensaje);
}
