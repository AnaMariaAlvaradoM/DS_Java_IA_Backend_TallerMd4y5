package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.servicio.dto.ServicioResponse;

import java.util.List;

public record CatalogoPublicoResponse(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioResponse> servicios,
        List<EstilistaPublicoDto> estilistas) {
}