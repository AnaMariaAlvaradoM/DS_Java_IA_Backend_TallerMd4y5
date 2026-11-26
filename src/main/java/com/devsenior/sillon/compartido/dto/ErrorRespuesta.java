package com.devsenior.sillon.compartido.dto;
import java.time.LocalDateTime;

public record ErrorRespuesta(
    LocalDateTime fecha,
    int codigo,
    String error,
    String mensaje) {

}
