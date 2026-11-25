package com.devsenior.sillon.usuario;

import java.util.List;

/** DTO de respuesta para un estilista del salon (nunca expone la contrasena). */
public record EstilistaRespuesta(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios) {
}
