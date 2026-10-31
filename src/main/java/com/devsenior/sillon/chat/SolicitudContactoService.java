package com.devsenior.sillon.chat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class SolicitudContactoService {

    private final SolicitudContactoRepository solicitudContactoRepository;
    private final SalonRepository salonRepository;
    private final Clock clock;

    public SolicitudContactoService(SolicitudContactoRepository solicitudContactoRepository,
            SalonRepository salonRepository, Clock clock) {
        this.solicitudContactoRepository = solicitudContactoRepository;
        this.salonRepository = salonRepository;
        this.clock = clock;
    }

    @Transactional
    public Long crear(Long salonId, String clienteNombre, String clienteContacto, String mensajes,
            MotivoSolicitud motivo) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(clienteContacto);
        solicitud.setMensajes(mensajes);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now(clock));
        solicitud.setSalon(salon);

        return solicitudContactoRepository.save(solicitud).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listarPorSalon(Long salonId) {
        return solicitudContactoRepository.findBySalonIdOrderByFechaCreacionDesc(salonId).stream()
                .map(SolicitudMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public SolicitudRespuesta marcarAtendida(Long salonId, Long id) {
        SolicitudContacto solicitud = solicitudContactoRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya fue atendida");
        }

        solicitud.setEstado(EstadoSolicitud.ATENDIDA);

        return SolicitudMapper.aRespuesta(solicitudContactoRepository.saveAndFlush(solicitud));
    }
}
