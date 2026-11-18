package com.devsenior.sillon.usuario.dto;

import java.util.List;

public record EstilistaResponse(Long id, String nombre, String email, boolean activo, List<String> servicios) {
}
