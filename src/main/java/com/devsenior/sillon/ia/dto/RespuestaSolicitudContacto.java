
package com.devsenior.sillon.ia.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;

public record RespuestaSolicitudContacto(

        Long id,

        String clienteNombre,

        String clienteContacto,

        List<SolicitudChatIa.MensajeHistorial> mensajes,

        MotivoSolicitud motivo,

        EstadoSolicitud estado,

        LocalDateTime fechaCreacion

) {
}
