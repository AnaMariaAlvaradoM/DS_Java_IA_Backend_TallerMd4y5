package com.devsenior.sillon.usuario.dto;

import java.util.List;

/** Nunca incluye la contraseña. Los servicios van como lista de nombres. */
public record EstilistaResponse(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios
) {
}
