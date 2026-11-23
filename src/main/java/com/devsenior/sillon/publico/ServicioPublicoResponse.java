
package com.devsenior.sillon.publico;

public record ServicioPublicoResponse(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int precio
) {
}
