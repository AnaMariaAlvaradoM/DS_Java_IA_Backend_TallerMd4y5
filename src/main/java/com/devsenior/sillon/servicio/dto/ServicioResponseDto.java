package com.devsenior.sillon.servicio.dto;


public record ServicioResponseDto(
        Long id,
        String nombre,
        String descripcion,
        Integer precio,
        Integer duracionMinutos,
        boolean activo
) {}