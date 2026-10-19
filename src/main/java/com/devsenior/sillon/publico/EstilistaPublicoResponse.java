package com.devsenior.sillon.publico;

import java.util.List;

public record EstilistaPublicoResponse(Long id, String nombre, List<Long> servicioIds) { }
