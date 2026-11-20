package com.devsenior.sillon.publico.dto;

import java.util.List;

public record EstilistaPublicoDTO(
        Long id,
        String nombre,
        List<Long> servicioIds
) {
}
