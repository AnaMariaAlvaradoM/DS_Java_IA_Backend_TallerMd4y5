package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;

import java.time.LocalDateTime;

public record CitaRespuesta(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fin,
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
        LocalDateTime fechaActualizacion
) {
}
