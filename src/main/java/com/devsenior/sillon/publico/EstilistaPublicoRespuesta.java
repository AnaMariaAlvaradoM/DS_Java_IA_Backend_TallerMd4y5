package com.devsenior.sillon.publico;

import java.util.List;

public record EstilistaPublicoRespuesta(
        Long id,
        String nombre,
        List<Long> servicioIds) {
}
