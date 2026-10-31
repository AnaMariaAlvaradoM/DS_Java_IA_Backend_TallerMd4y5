package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

public record CitaRespuesta(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoCita estado,
        OrigenCita origen,
        Long precio,
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
