package com.devsenior.sillon.publico;

import java.util.List;

public record CatalogoPublicoRespuesta(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioPublico> servicios,
        List<EstilistaPublico> estilistas) {

    public record ServicioPublico(Long id, String nombre, int duracionMinutos, long precio) {
    }

    public record EstilistaPublico(Long id, String nombre, List<Long> servicioIds) {
    }
}
