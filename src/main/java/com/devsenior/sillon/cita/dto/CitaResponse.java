package com.devsenior.sillon.cita.dto;

import static com.devsenior.sillon.cita.dto.CitaRequest.FORMATO_FECHA_HORA;

import java.time.LocalDateTime;

import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.fasterxml.jackson.annotation.JsonFormat;

public record CitaResponse(
        Long id,
        @JsonFormat(pattern = FORMATO_FECHA_HORA) LocalDateTime inicio,
        @JsonFormat(pattern = FORMATO_FECHA_HORA) LocalDateTime fin,
        EstadoCita estado,
        OrigenCita origen,
        int precio,
        Long servicioId,
        String servicioNombre,
        Long estilistaId,
        String estilistaNombre,
        String clienteNombre,
        String clienteTelefono,
        String nota,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}
