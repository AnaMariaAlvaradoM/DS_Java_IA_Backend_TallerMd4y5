package com.devsenior.sillon.usuario.estilista.dto;

import java.util.List;

public record EstilistaResponse(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios
) {
}
