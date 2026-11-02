package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;

import java.time.LocalDateTime;

public record CitaResponseDto(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fin,
        Integer precio,
        String nota,
        EstadoCita estado,
        OrigenCita origen,
        Long servicioId,
        String servicioNombre,
        Long estilistaId,
        String estilistaNombre,
        String clienteNombre,
        String clienteTelefono,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}