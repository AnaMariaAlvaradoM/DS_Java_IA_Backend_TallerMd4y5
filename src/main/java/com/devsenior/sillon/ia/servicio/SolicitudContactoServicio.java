
package com.devsenior.sillon.ia.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.dto.RespuestaSolicitudContacto;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.modelo.SolicitudContacto;
import com.devsenior.sillon.ia.repositorio.SolicitudContactoRepositorio;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class SolicitudContactoServicio {

    private final SolicitudContactoRepositorio solicitudRepositorio;
    private final SalonRepositorio salonRepositorio;
    private final ObjectMapper objectMapper;

    public SolicitudContactoServicio(
            SolicitudContactoRepositorio solicitudRepositorio,
            SalonRepositorio salonRepositorio,
            ObjectMapper objectMapper) {

        this.solicitudRepositorio = solicitudRepositorio;
        this.salonRepositorio = salonRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long crearSolicitud(
            Long salonId,
            String clienteNombre,
            String clienteContacto,
            List<SolicitudChatIa.MensajeHistorial> mensajes,
            MotivoSolicitud motivo) {

        Salon salon = salonRepositorio.findById(salonId)
                .orElseThrow(
                        () -> new ExcepcionRecursoNoEncontrado(
                                "Salon no encontrado"));

        SolicitudContacto solicitud = new SolicitudContacto();

        solicitud.setSalon(salon);
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(clienteContacto);
        solicitud.setMensajes(serializarMensajes(mensajes));
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        SolicitudContacto guardada = solicitudRepositorio.saveAndFlush(solicitud);

        return guardada.getId();
    }

    @Transactional(readOnly = true)
    public List<RespuestaSolicitudContacto> listar(Long salonId) {

        return solicitudRepositorio
                .buscarPorSalonOrdenadas(salonId)
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Transactional
    public RespuestaSolicitudContacto marcarAtendida(
            Long salonId,
            Long solicitudId) {

        SolicitudContacto solicitud = solicitudRepositorio
                .buscarPorIdYSalon(solicitudId, salonId)
                .orElseThrow(
                        () -> new ExcepcionRecursoNoEncontrado(
                                "Solicitud no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ExcepcionConflicto(
                    "La solicitud ya fue atendida");
        }

        solicitud.setEstado(EstadoSolicitud.ATENDIDA);

        solicitudRepositorio.saveAndFlush(solicitud);

        return convertirRespuesta(solicitud);
    }

    private RespuestaSolicitudContacto convertirRespuesta(
            SolicitudContacto solicitud) {

        return new RespuestaSolicitudContacto(
                solicitud.getId(),
                solicitud.getClienteNombre(),
                solicitud.getClienteContacto(),
                deserializarMensajes(solicitud.getMensajes()),
                solicitud.getMotivo(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion());
    }

    private String serializarMensajes(
            List<SolicitudChatIa.MensajeHistorial> mensajes) {

        try {

            return objectMapper.writeValueAsString(mensajes);

        } catch (JsonProcessingException excepcion) {

            throw new ExcepcionIntegracion(
                    "No fue posible serializar "
                            + "los mensajes de la solicitud");
        }
    }

    private List<SolicitudChatIa.MensajeHistorial> deserializarMensajes(
            String contenido) {

        try {

            return objectMapper.readValue(
                    contenido,
                    new TypeReference<List<SolicitudChatIa.MensajeHistorial>>() {
                    });

        } catch (JsonProcessingException excepcion) {

            throw new ExcepcionIntegracion(
                    "No fue posible recuperar "
                            + "los mensajes de la solicitud");
        }
    }
}
