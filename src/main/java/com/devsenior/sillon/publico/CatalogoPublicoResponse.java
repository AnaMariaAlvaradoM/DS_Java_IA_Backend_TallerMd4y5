package com.devsenior.sillon.publico;

import java.util.List;

public record CatalogoPublicoResponse(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioPublico> servicios,
        List<EstilistaPublico> estilistas) {

    public record ServicioPublico(
            Long id, String nombre, String descripcion,
            int duracionMinutos, int precio) {}

    public record EstilistaPublico(
            Long id, String nombre, List<Long> servicioIds) {}
}
