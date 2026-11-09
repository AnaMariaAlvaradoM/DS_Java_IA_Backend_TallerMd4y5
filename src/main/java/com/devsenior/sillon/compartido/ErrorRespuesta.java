package com.devsenior.sillon.compartido;

import java.time.OffsetDateTime;

public record ErrorRespuesta(int codigo, String error, String mensaje, OffsetDateTime fecha) {
}
