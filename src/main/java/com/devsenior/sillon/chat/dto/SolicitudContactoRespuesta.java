package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.chat.EstadoSolicitud;
import com.devsenior.sillon.chat.MotivoSolicitud;

import java.time.LocalDateTime;

public record SolicitudContactoRespuesta(
        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        MotivoSolicitud motivo,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion
) {
}
