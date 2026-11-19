package com.devsenior.sillon.publico.dto;

import com.devsenior.sillon.servicio.dto.ServicioResponse;
import java.util.List;

/** Cara pública del salón: sin correos, sin contraseñas, sin datos de planes ni créditos. */
public record CatalogoResponse(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioResponse> servicios,
        List<EstilistaPublicoResponse> estilistas
) {
}
