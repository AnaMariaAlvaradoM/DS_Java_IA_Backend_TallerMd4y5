package com.devSenior.sillon.publico.dto;

import java.util.List;

public record EstilistaPublico(Long id, String nombre, List<Long> servicioIds) {
}
