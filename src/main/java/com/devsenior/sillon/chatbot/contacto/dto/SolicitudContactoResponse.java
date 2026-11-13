package com.devsenior.sillon.chatbot.contacto.dto;

import com.devsenior.sillon.chatbot.contacto.model.EstadoSolicitud;
import com.devsenior.sillon.chatbot.contacto.model.MotivoSolicitud;

import java.time.LocalDateTime;

public record SolicitudContactoResponse(
        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        MotivoSolicitud motivo,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion
) {
}
