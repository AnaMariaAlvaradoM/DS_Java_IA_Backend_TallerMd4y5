package com.devsenior.sillon.usuario;

import java.util.List;

public record EstilistaRespuesta(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios) {
}
