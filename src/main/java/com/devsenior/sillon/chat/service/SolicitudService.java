package com.devsenior.sillon.chat.service;

import com.devsenior.sillon.chat.dto.SolicitudResponse;
import com.devsenior.sillon.chat.mapper.SolicitudMapper;
import com.devsenior.sillon.chat.model.EstadoSolicitud;
import com.devsenior.sillon.chat.model.MotivoSolicitud;
import com.devsenior.sillon.chat.model.SolicitudContacto;
import com.devsenior.sillon.chat.repository.SolicitudContactoRepository;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.repository.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudService {

    static final int MAXIMO_CARACTERES = 2000;

    private final SolicitudContactoRepository solicitudRepository;
    private final SalonRepository salonRepository;
    private final SolicitudMapper solicitudMapper;

    public SolicitudService(SolicitudContactoRepository solicitudRepository, SalonRepository salonRepository,
                            SolicitudMapper solicitudMapper) {
        this.solicitudRepository = solicitudRepository;
        this.salonRepository = salonRepository;
        this.solicitudMapper = solicitudMapper;
    }

    /** Transacción corta: se llama después de que la IA ya respondió. */
    @Transactional
    public Long crear(Long salonId, String clienteNombre, String clienteContacto, String mensajes, MotivoSolicitud motivo) {
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salonRepository.getReferenceById(salonId));
        solicitud.setClienteNombre(clienteNombre.trim());
        solicitud.setClienteContacto(clienteContacto.trim());
        solicitud.setMensajes(mensajes.length() > MAXIMO_CARACTERES ? mensajes.substring(0, MAXIMO_CARACTERES) : mensajes);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return solicitudRepository.save(solicitud).getId();
    }

    /** Las más recientes primero. */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(Long salonId) {
        return solicitudRepository.findBySalonIdOrderByFechaCreacionDescIdDesc(salonId).stream()
                .map(solicitudMapper::toResponse)
                .toList();
    }

    @Transactional
    public SolicitudResponse marcarAtendida(Long salonId, Long id) {
        SolicitudContacto solicitud = solicitudRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud con id " + id));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya estaba atendida");
        }
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return solicitudMapper.toResponse(solicitudRepository.save(solicitud));
    }
}
