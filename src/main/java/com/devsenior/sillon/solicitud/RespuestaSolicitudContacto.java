
package com.devsenior.sillon.solicitud;

import java.time.LocalDateTime;

public record RespuestaSolicitudContacto(

        Long id,
        String clienteNombre,
        String clienteContacto,
        String mensajes,
        String motivo,
        String estado,
        LocalDateTime fechaCreacion

) {

    public static RespuestaSolicitudContacto desde(
            SolicitudContacto solicitud
    ) {

        return new RespuestaSolicitudContacto(
                solicitud.getId(),
                solicitud.getClienteNombre(),
                solicitud.getClienteContacto(),
                solicitud.getMensajes(),
                solicitud.getMotivo().name(),
                solicitud.getEstado().name(),
                solicitud.getFechaCreacion()
        );
    }
}
