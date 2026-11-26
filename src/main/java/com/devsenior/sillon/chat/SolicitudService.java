package com.devsenior.sillon.chat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class SolicitudService {

    private static final int MAXIMO_MENSAJES = 2000;

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
        SolicitudContacto s = new SolicitudContacto();
        s.setSalon(salonRepository.getReferenceById(salonId));
        s.setClienteNombre(clienteNombre);
        s.setClienteContacto(clienteContacto);
        s.setMensajes(mensajes.length() > MAXIMO_MENSAJES ? mensajes.substring(0, MAXIMO_MENSAJES) : mensajes);
        s.setMotivo(motivo);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setFechaCreacion(LocalDateTime.now(clock));
        return solicitudRepository.save(s).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listar(Long salonId) {
        return solicitudRepository.findAllBySalonIdOrderByFechaCreacionDescIdDesc(salonId).stream()
                .map(SolicitudRespuesta::de)
                .toList();
    }

    @Transactional
    public SolicitudRespuesta marcarAtendida(Long salonId, Long id) {
        SolicitudContacto s = solicitudRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("La solicitud no existe"));
        if (s.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya estaba atendida");
        }
        s.setEstado(EstadoSolicitud.ATENDIDA);
        return SolicitudRespuesta.de(solicitudRepository.save(s));
    }
}
