package com.devsenior.sillon.publico;

import com.devsenior.sillon.servicio.ServicioResponse;
import java.util.List;

public record CatalogoResponse(String nombre, String slug, String ciudad, String direccion, String telefono,
        String horario, List<ServicioResponse> servicios, List<EstilistaPublicoResponse> estilistas) { }
