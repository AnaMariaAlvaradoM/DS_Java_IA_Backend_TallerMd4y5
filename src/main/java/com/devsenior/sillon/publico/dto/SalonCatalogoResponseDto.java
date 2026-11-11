package com.devsenior.sillon.publico.dto;

import com.devsenior.sillon.servicio.dto.ServicioResponseDto;
import java.util.List;

public record SalonCatalogoResponseDto(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioResponseDto> servicios,
        List<EstilistaPublicoDto> estilistas
) {}