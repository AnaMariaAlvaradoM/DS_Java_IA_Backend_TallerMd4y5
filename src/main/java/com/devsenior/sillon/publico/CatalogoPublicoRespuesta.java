package com.devsenior.sillon.publico;

import java.util.List;

import com.devsenior.sillon.servicio.ServicioRespuesta;

/**
 * Catalogo publico del salon para clientes finales.
 * Expone informacion de contacto, horario legible en texto,
 * catalogo de servicios activos y estilistas activos con sus servicios.
 * NUNCA incluye correos electronicos ni contrasenas.
 */
public record CatalogoPublicoRespuesta(
        String nombre,
        String slug,
        String ciudad,
        String direccion,
        String telefono,
        String horario,
        List<ServicioRespuesta> servicios,
        List<EstilistaPublicoRespuesta> estilistas) {
}
