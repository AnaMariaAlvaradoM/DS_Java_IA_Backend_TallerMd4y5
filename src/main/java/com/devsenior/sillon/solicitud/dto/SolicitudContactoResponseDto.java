package com.devsenior.sillon.solicitud.dto;

import com.devsenior.sillon.solicitud.EstadoSolicitud;
import com.devsenior.sillon.solicitud.MotivoSolicitud;
import com.devsenior.sillon.solicitud.SolicitudContacto;

import java.time.LocalDateTime;

public record SolicitudContactoResponseDto(
        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        MotivoSolicitud motivo,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion
) {
    public static SolicitudContactoResponseDto desde(SolicitudContacto s) {
        return new SolicitudContactoResponseDto(
                s.getId(),
                s.getClienteNombre(),
                s.getClienteContacto(),
                s.getMensajes(),
                s.getMotivo(),
                s.getEstado(),
                s.getFechaCreacion()
        );
    }
}
