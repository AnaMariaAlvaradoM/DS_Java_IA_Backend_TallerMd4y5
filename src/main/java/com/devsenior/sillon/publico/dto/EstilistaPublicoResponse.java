package com.devsenior.sillon.publico.dto;

import java.util.List;

/** Lo único que el cliente final ve de un estilista. Nunca el correo. */
public record EstilistaPublicoResponse(Long id, String nombre, List<Long> servicioIds) {
}
