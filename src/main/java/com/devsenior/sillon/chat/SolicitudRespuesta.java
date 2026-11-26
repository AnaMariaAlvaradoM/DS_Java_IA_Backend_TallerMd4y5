package com.devsenior.sillon.chat;

import java.time.LocalDateTime;

public record SolicitudRespuesta(Long id, String clienteNombre, String clienteContacto, String mensajes,
        MotivoSolicitud motivo, EstadoSolicitud estado, LocalDateTime fechaCreacion) {

    public static SolicitudRespuesta de(SolicitudContacto s) {
        return new SolicitudRespuesta(s.getId(), s.getClienteNombre(), s.getClienteContacto(), s.getMensajes(),
                s.getMotivo(), s.getEstado(), s.getFechaCreacion());
    }
}
