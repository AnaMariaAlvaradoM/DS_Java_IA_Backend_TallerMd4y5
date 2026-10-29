package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;

    public ServicioService(ServicioRepository servicioRepository, SalonRepository salonRepository) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public ServicioRespuesta crear(Long salonId, ServicioRequest request) {
        Servicio servicio = new Servicio();
        servicio.setSalon(salonRepository.getReferenceById(salonId));
        aplicarDatos(servicio, request);
        Servicio guardado = servicioRepository.save(servicio);
        return ServicioMapper.aRespuesta(guardado);
    }

    @Transactional
    public ServicioRespuesta actualizar(Long salonId, Long id, ServicioRequest request) {
        Servicio servicio = buscar(salonId, id);
        aplicarDatos(servicio, request);
        Servicio actualizado = servicioRepository.save(servicio);
        return ServicioMapper.aRespuesta(actualizado);
    }

    public List<ServicioRespuesta> listarPorSalon(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(ServicioMapper::aRespuesta)
                .toList();
    }

    private void aplicarDatos(Servicio servicio, ServicioRequest request) {
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo() == null ? true : request.activo());
    }

    private Servicio buscar(Long salonId, Long id) {
        return servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    }
}
