package com.devsenior.sillon.compartido.dto;
import java.time.LocalDateTime;

public record ErrorRespuesta(
    LocalDateTime fechaHora,
    int status,
    String error,
    String mensaje) {

}
