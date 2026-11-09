package com.devsenior.sillon.publico;

import com.devsenior.sillon.servicio.ServicioRespuesta;
import java.util.List;

public record CatalogoRespuesta(String nombre, String slug, String ciudad, String direccion,
        String telefono, String horario, List<ServicioRespuesta> servicios,
        List<EstilistaPublicoRespuesta> estilistas) {
}
