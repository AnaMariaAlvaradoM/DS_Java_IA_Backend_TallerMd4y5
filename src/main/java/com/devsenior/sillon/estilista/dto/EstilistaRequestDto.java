package com.devsenior.sillon.estilista.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record EstilistaRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String especialidad,

        List<Long> serviciosIds
) {}