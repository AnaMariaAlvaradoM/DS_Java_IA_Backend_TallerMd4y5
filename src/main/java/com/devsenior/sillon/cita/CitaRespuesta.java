package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

/**
 * Representacion completa de una cita del salon.
 * Contiene todos los campos del contrato acordado.
 */
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
        LocalDateTime fechaActualizacion) {
}
