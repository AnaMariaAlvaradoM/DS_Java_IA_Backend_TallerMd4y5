package com.devsenior.sillon.estilista.dto;

import java.util.List;

public record EstilistaResponseDto(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios
) {}