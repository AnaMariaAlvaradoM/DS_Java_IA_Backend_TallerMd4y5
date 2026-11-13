package com.devsenior.sillon.chatbot.contacto.service;

import com.devsenior.sillon.chatbot.contacto.dto.SolicitudContactoResponse;
import com.devsenior.sillon.chatbot.contacto.model.EstadoSolicitud;
import com.devsenior.sillon.chatbot.contacto.model.MotivoSolicitud;
import com.devsenior.sillon.chatbot.contacto.model.SolicitudContacto;
import com.devsenior.sillon.chatbot.contacto.repository.SolicitudContactoRepository;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class SolicitudContactoService {
    private final SolicitudContactoRepository solicitudContactoRepository;
    private final SalonRepository salonRepository;
    public SolicitudContactoService(SolicitudContactoRepository solicitudContactoRepository, SalonRepository salonRepository) {
        this.solicitudContactoRepository = solicitudContactoRepository;
        this.salonRepository = salonRepository;
    }
    @Transactional
    public SolicitudContacto crearSolicitud(String slug,
                                            String clienteNombre,
                                            String clienteContacto,
                                            String mensajesCliente,
                                            MotivoSolicitud motivo) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        String mensajesSeguros = (mensajesCliente != null && mensajesCliente.length() > 2000)
                ? mensajesCliente.substring(0, 2000)
                : (mensajesCliente != null ? mensajesCliente : "");

        String contactoSeguro = (clienteContacto != null && !clienteContacto.isBlank())
                ? clienteContacto
                : "No proporcionado";

        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setSalonId(salon.getId());
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(contactoSeguro);
        solicitud.setMensajes(mensajesSeguros);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now());

        return solicitudContactoRepository.save(solicitud);
    }
    @Transactional(readOnly = true)
    public List<SolicitudContactoResponse> obtenerSolicitudes(Long salonId) {
        return solicitudContactoRepository.findBySalonIdOrderByFechaCreacionDesc(salonId)
                .stream()
                .map(s -> new SolicitudContactoResponse(
                        s.getId(),
                        s.getClienteNombre(),
                        s.getClienteContacto(),
                        s.getMensajes(),
                        s.getMotivo(),
                        s.getEstado(),
                        s.getFechaCreacion()
                ))
                .toList();
    }
    @Transactional
    public void marcarComoAtendida(Long id, Long salonId) {
        // 1. Buscar la solicitud (Si no existe lanza 404)
        SolicitudContacto solicitud = solicitudContactoRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Solicitud no encontrada"));

        // 2. Regla de negocio: Si ya está atendida, lanzar 409
        if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
            throw new ReglaNegocioException("La solicitud ya se encuentra atendida");
        }

        // 3. Actualizar estado y guardar
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitudContactoRepository.save(solicitud);
    }
}