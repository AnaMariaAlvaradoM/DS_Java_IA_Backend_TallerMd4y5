package com.devsenior.sillon.publico.dto;

import java.util.List;

public record EstilistaResponse1(
        Long id,
        String nombre,
        List<Long> serviciosids
) {
}
