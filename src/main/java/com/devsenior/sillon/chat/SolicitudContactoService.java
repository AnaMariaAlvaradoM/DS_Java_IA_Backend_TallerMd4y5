package com.devsenior.sillon.chat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.chat.dto.SolicitudResponse;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class SolicitudContactoService {

    private final SolicitudContactoRepository solicitudes;
    private final SalonRepository salones;
    private final Clock clock;

    public SolicitudContactoService(SolicitudContactoRepository solicitudes, SalonRepository salones, Clock clock) {
        this.solicitudes = solicitudes;
        this.salones = salones;
        this.clock = clock;
    }

    @Transactional
    public Long crear(Long salonId, String nombre, String contacto, String mensajes, MotivoSolicitud motivo) {
        var salon = salones.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        String resumen = mensajes.substring(0, mensajes.offsetByCodePoints(0,
                Math.min(2000, mensajes.codePointCount(0, mensajes.length()))));
        return solicitudes.saveAndFlush(new SolicitudContacto(salon, nombre, contacto, resumen, motivo,
                LocalDateTime.now(clock))).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(Long salonId) {
        return solicitudes.findAllBySalonIdOrderByFechaCreacionDescIdDesc(salonId).stream()
                .map(SolicitudContactoService::respuesta).toList();
    }

    @Transactional
    public SolicitudResponse atender(Long salonId, Long id) {
        var solicitud = solicitudes.findOneByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaDeNegocioException("La solicitud ya fue atendida");
        }
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return respuesta(solicitudes.saveAndFlush(solicitud));
    }

    private static SolicitudResponse respuesta(SolicitudContacto solicitud) {
        return new SolicitudResponse(solicitud.getId(), solicitud.getClienteNombre(), solicitud.getClienteContacto(),
                solicitud.getMensajes(), solicitud.getMotivo(), solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
