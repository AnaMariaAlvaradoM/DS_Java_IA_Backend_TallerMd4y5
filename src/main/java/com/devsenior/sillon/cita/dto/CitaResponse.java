package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.model.EstadoCita;
import com.devsenior.sillon.cita.model.OrigenCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoCita estado,
        OrigenCita origen,
        Integer precio,
        Long servicioId,
        String servicioNombre,
        Long estilistaId,
        String estilistaNombre,
        String clienteNombre,
        String clienteTelefono,
        String nota,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
