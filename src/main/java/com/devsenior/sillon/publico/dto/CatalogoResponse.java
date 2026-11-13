package com.devsenior.sillon.publico.dto;

import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaResponse;

import java.util.List;

//nombre, slug, ciudad, direccion,
//telefono, horario (texto), servicios (solo
//activos) y estilistas (id, nombre, servicioIds).
//Nunca correos
public record CatalogoResponse(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioResponse> servicios,
        List<EstilistaResponse1> estilistas
) {
}
