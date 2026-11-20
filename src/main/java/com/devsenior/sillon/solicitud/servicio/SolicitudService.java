package com.devsenior.sillon.solicitud.servicio;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.solicitud.dto.SolicitudResponse;
import com.devsenior.sillon.solicitud.modelo.EstadoSolicitud;
import com.devsenior.sillon.solicitud.modelo.MotivoSolicitud;
import com.devsenior.sillon.solicitud.modelo.Solicitud;
import com.devsenior.sillon.solicitud.repositorio.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final SalonRepository salonRepository;
    private final Clock clock;

    public SolicitudService(SolicitudRepository solicitudRepository, SalonRepository salonRepository, Clock clock) {
        this.solicitudRepository = solicitudRepository;
        this.salonRepository = salonRepository;
        this.clock = clock;
    }

    @Transactional
    public Long crear(Long salonId, String clienteNombre, String clienteContacto, String mensajes,
            MotivoSolicitud motivo) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        String textoRecortado = mensajes.length() > 2000 ? mensajes.substring(0, 2000) : mensajes;

        Solicitud solicitud = new Solicitud();
        solicitud.setSalon(salon);
        solicitud.setClienteNombre(clienteNombre.trim());
        solicitud.setClienteContacto(clienteContacto.trim());
        solicitud.setMensajes(textoRecortado);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now(clock));

        return solicitudRepository.save(solicitud).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> listarPorSalon(Long salonId) {
        return solicitudRepository.findBySalonIdOrderByFechaCreacionDesc(salonId).stream()
                .map(SolicitudResponse::desde)
                .toList();
    }

    @Transactional
    public SolicitudResponse marcarAtendida(Long salonId, Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findByIdAndSalonId(solicitudId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya fue marcada como atendida");
        }

        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return SolicitudResponse.desde(solicitudRepository.save(solicitud));
    }
}