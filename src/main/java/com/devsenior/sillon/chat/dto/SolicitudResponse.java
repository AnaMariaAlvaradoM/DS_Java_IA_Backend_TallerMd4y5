package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.chat.model.EstadoSolicitud;
import com.devsenior.sillon.chat.model.MotivoSolicitud;
import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        MotivoSolicitud motivo,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion
) {
}
