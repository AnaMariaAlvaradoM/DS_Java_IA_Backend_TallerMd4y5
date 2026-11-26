package com.devsenior.sillon.usuario;

import java.util.List;

/** Nunca incluye la contraseña. Los servicios van por nombre. */
public record EstilistaRespuesta(Long id, String nombre, String email, boolean activo, List<String> servicios) {
}
