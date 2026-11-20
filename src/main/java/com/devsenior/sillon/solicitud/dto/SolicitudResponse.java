package com.devsenior.sillon.solicitud.dto;

import com.devsenior.sillon.solicitud.modelo.EstadoSolicitud;
import com.devsenior.sillon.solicitud.modelo.MotivoSolicitud;
import com.devsenior.sillon.solicitud.modelo.Solicitud;

import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        MotivoSolicitud motivo,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion) {
    public static SolicitudResponse desde(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getClienteNombre(),
                solicitud.getClienteContacto(),
                solicitud.getMensajes(),
                solicitud.getMotivo(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion());
    }
}