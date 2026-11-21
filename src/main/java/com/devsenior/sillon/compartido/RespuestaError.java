
package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;

public record RespuestaError(
    int codigo,
    String error,
    String mensaje,
    LocalDateTime fecha
) {
}
