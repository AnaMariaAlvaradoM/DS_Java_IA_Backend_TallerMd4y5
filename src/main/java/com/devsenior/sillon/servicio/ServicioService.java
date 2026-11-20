package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.dto.CrearServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;

    public ServicioService(ServicioRepository servicioRepository, SalonRepository salonRepository) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public ServicioRespuesta crearServicio(Long salonId, CrearServicioRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Servicio servicio = new Servicio();
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo() != null ? request.activo() : true);
        servicio.setSalon(salon);

        servicio = servicioRepository.save(servicio);
        return mapearARespuesta(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listarServicios(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    @Transactional
    public ServicioRespuesta actualizarServicio(Long salonId, Long servicioId, CrearServicioRequest request) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado en este salón"));

        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        if (request.activo() != null) {
            servicio.setActivo(request.activo());
        }

        servicio = servicioRepository.save(servicio);
        return mapearARespuesta(servicio);
    }

    public ServicioRespuesta mapearARespuesta(Servicio servicio) {
        return new ServicioRespuesta(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.isActivo()
        );
    }
}
