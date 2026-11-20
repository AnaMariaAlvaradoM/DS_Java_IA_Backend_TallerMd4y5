package com.devsenior.sillon.publico.dto;

import java.util.List;

public record CatalogoSalonRespuesta(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioPublicoDTO> servicios,
        List<EstilistaPublicoDTO> estilistas
) {
}
