package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudContactoServicio {
    private final SolicitudContactoRepositorio solicitudes;
    private final SalonRepositorio salones;
    private final Clock clock;

    public SolicitudContactoServicio(SolicitudContactoRepositorio solicitudes, SalonRepositorio salones, Clock clock) {
        this.solicitudes = solicitudes;
        this.salones = salones;
        this.clock = clock;
    }

    @Transactional
    public Long crear(Long salonId, String nombre, String contacto, String mensajes, MotivoSolicitud motivo) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salon);
        solicitud.setClienteNombre(nombre);
        solicitud.setClienteContacto(contacto);
        solicitud.setMensajes(mensajes);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now(clock));
        return solicitudes.save(solicitud).getId();
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoRespuesta> listar(Long salonId) {
        return solicitudes.findAllBySalon_IdOrderByFechaCreacionDescIdDesc(salonId)
                .stream().map(SolicitudContactoRespuesta::desde).toList();
    }

    @Transactional
    public SolicitudContactoRespuesta marcarAtendida(Long id, Long salonId) {
        SolicitudContacto solicitud = solicitudes.findByIdAndSalon_Id(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Solicitud no encontrada"));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA)
            throw new ReglaNegocioException("La solicitud ya fue atendida");
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return SolicitudContactoRespuesta.desde(solicitudes.save(solicitud));
    }
}
