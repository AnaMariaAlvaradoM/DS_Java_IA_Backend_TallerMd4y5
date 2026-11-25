package com.devsenior.sillon.chat;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;

@Service
public class SolicitudServicio {

    private final SolicitudContactoRepository solicitudRepository;

    public SolicitudServicio(SolicitudContactoRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoRespuesta> listarSolicitudes(Long salonId) {
        return solicitudRepository.findBySalonIdOrderByFechaCreacionDesc(salonId).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public SolicitudContactoRespuesta marcarAtendida(Long id, Long salonId) {
        SolicitudContacto solicitud = solicitudRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada o pertenece a otro salón"));

        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya ha sido atendida previamente.");
        }

        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        SolicitudContacto guardada = solicitudRepository.save(solicitud);

        return aRespuesta(guardada);
    }

    private SolicitudContactoRespuesta aRespuesta(SolicitudContacto s) {
        return new SolicitudContactoRespuesta(
                s.getId(),
                s.getClienteNombre(),
                s.getClienteContacto(),
                s.getMensajes(),
                s.getMotivo(),
                s.getEstado(),
                s.getFechaCreacion()
        );
    }
}
