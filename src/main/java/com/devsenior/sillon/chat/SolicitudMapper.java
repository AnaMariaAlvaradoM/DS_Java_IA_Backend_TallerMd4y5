package com.devsenior.sillon.chat;

import org.springframework.stereotype.Component;

@Component
public class SolicitudMapper {
    public SolicitudResponse convertir(SolicitudContacto solicitud) {
        return new SolicitudResponse(solicitud.getId(), solicitud.getClienteNombre(), solicitud.getClienteContacto(),
                solicitud.getMensajes(), solicitud.getMotivo(), solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
