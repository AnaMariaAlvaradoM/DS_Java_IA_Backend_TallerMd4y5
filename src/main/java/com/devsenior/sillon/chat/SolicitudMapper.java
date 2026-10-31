package com.devsenior.sillon.chat;

public final class SolicitudMapper {

    private SolicitudMapper() {
    }

    public static SolicitudRespuesta aRespuesta(SolicitudContacto solicitud) {
        return new SolicitudRespuesta(solicitud.getId(), solicitud.getClienteNombre(),
                solicitud.getClienteContacto(), solicitud.getMensajes(), solicitud.getMotivo(),
                solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
