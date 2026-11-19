package com.devsenior.sillon.chat.dto;

import java.time.LocalDateTime;

import com.devsenior.sillon.chat.EstadoSolicitud;
import com.devsenior.sillon.chat.MotivoSolicitud;

public record SolicitudResponse(Long id, String clienteNombre, String clienteContacto, String mensajes,
        MotivoSolicitud motivo, EstadoSolicitud estado, LocalDateTime fechaCreacion) {
}
