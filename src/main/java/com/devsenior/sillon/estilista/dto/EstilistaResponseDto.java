package com.devsenior.sillon.estilista.dto;

import java.util.List;

public record EstilistaResponseDto(
        Long id,
        String nombre,
        String especialidad,
        boolean activo,
        List<Long> serviciosIds
) {}