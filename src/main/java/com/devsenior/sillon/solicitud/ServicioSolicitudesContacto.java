
package com.devsenior.sillon.solicitud;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ServicioSolicitudesContacto {

    private final SolicitudContactoRepository solicitudRepository;
    private final SalonRepository salonRepository;

    public ServicioSolicitudesContacto(
            SolicitudContactoRepository solicitudRepository,
            SalonRepository salonRepository
    ) {
        this.solicitudRepository = solicitudRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public SolicitudContacto crear(
            Long salonId,
            String clienteNombre,
            String clienteContacto,
            String mensajes,
            SolicitudContacto.Motivo motivo
    ) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Salón no encontrado"
                        )
                );

        if (mensajes == null || mensajes.isBlank()) {
            throw new IllegalArgumentException(
                    "Los mensajes del cliente son obligatorios"
            );
        }

        String mensajesLimitados = mensajes.substring(
                0,
                Math.min(mensajes.length(), 2000)
        );

        SolicitudContacto solicitud = new SolicitudContacto();

        solicitud.setSalon(salon);
        solicitud.setClienteNombre(clienteNombre);
        solicitud.setClienteContacto(clienteContacto);
        solicitud.setMensajes(mensajesLimitados);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(
                SolicitudContacto.Estado.PENDIENTE
        );

        return solicitudRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudContacto> listar(Long salonId) {

        if (!salonRepository.existsById(salonId)) {
            throw new RecursoNoEncontradoException(
                    "Salón no encontrado"
            );
        }

        return solicitudRepository
                .findBySalonIdOrderByFechaCreacionDescIdDesc(
                        salonId
                );
    }

    @Transactional
    public SolicitudContacto marcarAtendida(
            Long salonId,
            Long solicitudId
    ) {

        SolicitudContacto solicitud = solicitudRepository
                .findByIdAndSalonId(solicitudId, salonId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Solicitud de contacto no encontrada"
                        )
                );

        if (solicitud.getEstado()
                == SolicitudContacto.Estado.ATENDIDA) {

            throw new ReglaNegocioException(
                    "La solicitud ya fue atendida"
            );
        }

        solicitud.setEstado(
                SolicitudContacto.Estado.ATENDIDA
        );

        return solicitudRepository.save(solicitud);
    }
}
