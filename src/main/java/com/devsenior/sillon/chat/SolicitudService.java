package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudService {
    private final SolicitudRepository solicitudes;
    private final SalonRepository salones;
    private final SolicitudMapper mapper;

    public SolicitudService(SolicitudRepository solicitudes, SalonRepository salones, SolicitudMapper mapper) {
        this.solicitudes = solicitudes;
        this.salones = salones;
        this.mapper = mapper;
    }

    @Transactional
    public SolicitudResponse crear(Long salonId, String nombre, String contacto, String mensajes, MotivoSolicitud motivo) {
        var salon = salones.findById(salonId).orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalon(salon); solicitud.setClienteNombre(nombre); solicitud.setClienteContacto(contacto);
        int limite = Math.min(mensajes.length(), 2000);
        if (limite < mensajes.length() && Character.isHighSurrogate(mensajes.charAt(limite - 1))
                && Character.isLowSurrogate(mensajes.charAt(limite))) {
            limite--;
        }
        solicitud.setMensajes(mensajes.substring(0, limite));
        solicitud.setMotivo(motivo); solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return mapper.convertir(solicitudes.saveAndFlush(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(Long salonId) {
        return solicitudes.findAllBySalonIdOrderByFechaCreacionDescIdDesc(salonId).stream().map(mapper::convertir).toList();
    }

    @Transactional
    public SolicitudResponse atender(Long salonId, Long id) {
        SolicitudContacto solicitud = solicitudes.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya fue atendida");
        }
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        return mapper.convertir(solicitudes.saveAndFlush(solicitud));
    }
}
