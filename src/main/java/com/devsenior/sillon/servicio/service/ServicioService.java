package com.devsenior.sillon.servicio.service;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.servicio.repository.ServicioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final ServicioMapper servicioMapper;
    private final SalonService salonService;

    public ServicioService(ServicioRepository servicioRepository, ServicioMapper servicioMapper,
                           SalonService salonService) {
        this.servicioRepository = servicioRepository;
        this.servicioMapper = servicioMapper;
        this.salonService = salonService;
    }

    @Transactional
    public ServicioResponse crear(Long salonId, ServicioRequest request) {
        Servicio servicio = new Servicio();
        servicioMapper.copiar(request, servicio);
        servicio.setSalon(salonService.obtenerEntidad(salonId));
        return servicioMapper.toResponse(servicioRepository.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long salonId) {
        return servicioRepository.findBySalonIdOrderByNombreAsc(salonId).stream()
                .map(servicioMapper::toResponse)
                .toList();
    }

    @Transactional
    public ServicioResponse actualizar(Long salonId, Long id, ServicioRequest request) {
        Servicio servicio = obtenerEntidad(salonId, id);
        servicioMapper.copiar(request, servicio);
        return servicioMapper.toResponse(servicioRepository.save(servicio));
    }

    /** Busca por id y salón a la vez: si es de otro salón, no aparece (404). */
    @Transactional(readOnly = true)
    public Servicio obtenerEntidad(Long salonId, Long id) {
        return servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el servicio con id " + id));
    }
}
