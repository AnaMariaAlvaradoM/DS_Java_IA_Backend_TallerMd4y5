package com.devsenior.sillon.publico.dto;

import java.util.List;

public record EstilistaPublicoDto(
        Long id,
        String nombre,
        List<Long> servicioIds
) {}
