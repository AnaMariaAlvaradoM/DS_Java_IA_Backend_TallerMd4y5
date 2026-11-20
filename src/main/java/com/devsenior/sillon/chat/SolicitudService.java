package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudService {
    private final SolicitudContactoRepository solicitudes;
    private final SalonRepository salones;
    private final Clock reloj;

    public SolicitudService(SolicitudContactoRepository solicitudes, SalonRepository salones, Clock reloj) {
        this.solicitudes = solicitudes;
        this.salones = salones;
        this.reloj = reloj;
    }

    @Transactional
    public Long crear(Long salonId, String nombre, String contacto, String mensajes, MotivoSolicitud motivo) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salon);
        solicitud.setClienteNombre(nombre);
        solicitud.setClienteContacto(contacto);
        solicitud.setMensajes(mensajes.substring(0, Math.min(2000, mensajes.length())));
        solicitud.setMotivo(motivo);
        solicitud.setFechaCreacion(LocalDateTime.now(reloj));
        return solicitudes.save(solicitud).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listar(Long salonId) {
        return solicitudes.findBySalonIdOrderByFechaCreacionDesc(salonId).stream().map(this::mapear).toList();
    }

    @Transactional
    public SolicitudRespuesta atender(Long salonId, Long solicitudId) {
        SolicitudContacto solicitud = solicitudes.findByIdAndSalonId(solicitudId, salonId)
                .orElseThrow(() -> new NoEncontradoException("Solicitud no encontrada"));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya fue atendida");
        }
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return mapear(solicitudes.save(solicitud));
    }

    private SolicitudRespuesta mapear(SolicitudContacto solicitud) {
        return new SolicitudRespuesta(solicitud.getId(), solicitud.getClienteNombre(),
                solicitud.getClienteContacto(), solicitud.getMensajes(), solicitud.getMotivo(),
                solicitud.getEstado(), solicitud.getFechaCreacion());
    }
}
