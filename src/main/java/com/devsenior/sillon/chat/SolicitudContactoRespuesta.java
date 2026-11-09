package com.devsenior.sillon.chat;

import java.time.LocalDateTime;

public record SolicitudContactoRespuesta(Long id, String clienteNombre, String clienteContacto,
                                        String mensajes, MotivoSolicitud motivo,
                                        EstadoSolicitud estado, LocalDateTime fechaCreacion) {
    public static SolicitudContactoRespuesta desde(SolicitudContacto solicitud) {
        return new SolicitudContactoRespuesta(solicitud.getId(), solicitud.getClienteNombre(),
                solicitud.getClienteContacto(), solicitud.getMensajes(), solicitud.getMotivo(),
                solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
