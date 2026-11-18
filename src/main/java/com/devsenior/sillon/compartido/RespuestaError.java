package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;

/**
 * Formato único de error del contrato (SPEC.md §7.3): todos los errores,
 * incluidos 401 y 403, responden con estos cuatro campos.
 */
public record RespuestaError(int codigo, String error, String mensaje, LocalDateTime fecha) {
}
