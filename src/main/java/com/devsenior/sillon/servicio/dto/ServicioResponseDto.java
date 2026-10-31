package com.devsenior.sillon.servicio.dto;

import java.math.BigDecimal;

public record ServicioResponseDto(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        boolean activo
) {}