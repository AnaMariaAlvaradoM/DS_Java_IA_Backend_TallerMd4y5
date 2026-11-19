package com.devsenior.sillon.cita.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CitaRequest(
        @NotBlank(message = "es obligatorio") String clienteNombre,
        @NotBlank(message = "es obligatorio")
        @Size(min = 7, max = 30, message = "debe tener entre 7 y 30 caracteres") String clienteTelefono,
        @NotNull(message = "es obligatorio") Long servicioId,
        @NotNull(message = "es obligatorio") Long estilistaId,
        @NotNull(message = "es obligatorio")
        @JsonFormat(pattern = FORMATO_FECHA_HORA, lenient = OptBoolean.FALSE) LocalDateTime inicio,
        @Size(max = 300, message = "debe tener como máximo 300 caracteres") String nota) {

    static final String FORMATO_FECHA_HORA = "uuuu-MM-dd'T'HH:mm:ss";
}
