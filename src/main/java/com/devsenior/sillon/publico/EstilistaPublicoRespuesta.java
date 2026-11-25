package com.devsenior.sillon.publico;

import java.util.List;

/**
 * Representacion publica de un estilista.
 * Solo expone id, nombre y los ids de los servicios que ofrece.
 * NUNCA expone correos ni contrasenas.
 */
public record EstilistaPublicoRespuesta(
        Long id,
        String nombre,
        List<Long> servicioIds) {
}
