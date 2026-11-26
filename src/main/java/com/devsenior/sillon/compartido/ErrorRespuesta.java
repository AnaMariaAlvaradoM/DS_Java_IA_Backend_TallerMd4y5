package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;

/** Formato único de error de toda la API. */
public record ErrorRespuesta(int codigo, String error, String mensaje, LocalDateTime fecha) {
}
