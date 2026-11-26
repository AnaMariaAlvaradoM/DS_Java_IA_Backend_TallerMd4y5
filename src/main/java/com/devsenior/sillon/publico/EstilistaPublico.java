package com.devsenior.sillon.publico;

import java.util.List;

public record EstilistaPublico(Long id, String nombre, List<Long> servicioIds) {
}
