package com.devsenior.sillon.chat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class SolicitudServicio {

    private static final String MENSAJE_SOLICITUD_NO_ENCONTRADA = "Solicitud no encontrada";
    private static final String MENSAJE_SALON_NO_ENCONTRADO = "Salón no encontrado";

    private final SolicitudContactoRepository solicitudRepository;
    private final SalonRepository salonRepository;
    private final Clock reloj;

    public SolicitudServicio(SolicitudContactoRepository solicitudRepository,
            SalonRepository salonRepository, Clock reloj) {
        this.solicitudRepository = solicitudRepository;
        this.salonRepository = salonRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listar(Long salonId) {
        return solicitudRepository.findBySalonIdOrderByFechaCreacionDesc(salonId).stream()
                .map(SolicitudServicio::aRespuesta)
                .toList();
    }

    @Transactional
    public SolicitudRespuesta atender(Long salonId, Long id) {
        SolicitudContacto solicitud = solicitudRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SOLICITUD_NO_ENCONTRADA));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya estaba atendida");
        }
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitud = solicitudRepository.saveAndFlush(solicitud);
        return aRespuesta(solicitud);
    }

    @Transactional
    public Long crear(Long salonId, String clienteNombre, String clienteContacto, String mensajes,
            MotivoSolicitud motivo) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SALON_NO_ENCONTRADO));
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salon);
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(clienteContacto);
        solicitud.setMensajes(mensajes);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now(reloj));
        return solicitudRepository.save(solicitud).getId();
    }

    private static SolicitudRespuesta aRespuesta(SolicitudContacto solicitud) {
        return new SolicitudRespuesta(
                solicitud.getId(),
                solicitud.getClienteNombre(),
                solicitud.getClienteContacto(),
                solicitud.getMensajes(),
                solicitud.getMotivo(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion());
    }
}
