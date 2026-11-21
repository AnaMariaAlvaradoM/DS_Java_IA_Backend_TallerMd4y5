package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioService {
    private final ServicioRepository servicios;
    private final SalonRepository salones;
    private final ServicioMapper mapper;

    public ServicioService(ServicioRepository servicios, SalonRepository salones, ServicioMapper mapper) {
        this.servicios = servicios;
        this.salones = salones;
        this.mapper = mapper;
    }

    @Transactional
    public ServicioRespuesta crear(Long salonId, ServicioSolicitud solicitud) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        Servicio servicio = new Servicio();
        servicio.setSalon(salon);
        aplicar(servicio, solicitud);
        return mapper.aRespuesta(servicios.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(Long salonId) {
        return servicios.findAllBySalonIdOrderByIdAsc(salonId).stream()
                .map(mapper::aRespuesta).toList();
    }

    @Transactional
    public ServicioRespuesta actualizar(Long salonId, Long id, ServicioSolicitud solicitud) {
        Servicio servicio = servicios.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        aplicar(servicio, solicitud);
        return mapper.aRespuesta(servicio);
    }

    private void aplicar(Servicio servicio, ServicioSolicitud solicitud) {
        servicio.setNombre(solicitud.nombre().trim());
        servicio.setDescripcion(solicitud.descripcion());
        servicio.setDuracionMinutos(solicitud.duracionMinutos());
        servicio.setPrecio(solicitud.precio());
        servicio.setActivo(solicitud.activo() == null || solicitud.activo());
    }
}
