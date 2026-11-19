package com.devsenior.sillon.chat.mapper;

import com.devsenior.sillon.chat.dto.SolicitudResponse;
import com.devsenior.sillon.chat.model.SolicitudContacto;
import org.springframework.stereotype.Component;

@Component
public class SolicitudMapper {

    public SolicitudResponse toResponse(SolicitudContacto solicitud) {
        return new SolicitudResponse(solicitud.getId(), solicitud.getClienteNombre(), solicitud.getClienteContacto(),
                solicitud.getMensajes(), solicitud.getMotivo(), solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
