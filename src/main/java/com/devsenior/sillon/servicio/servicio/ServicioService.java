package com.devsenior.sillon.servicio.servicio;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;
    private final ServicioMapper servicioMapper;

    public ServicioService(
            ServicioRepository servicioRepository,
            SalonRepository salonRepository,
            ServicioMapper servicioMapper) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
        this.servicioMapper = servicioMapper;
    }

    @Transactional
    public ServicioResponse crear(Long salonId, ServicioRequest request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Servicio servicio = new Servicio();
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo() != null ? request.activo() : true);
        servicio.setSalon(salon);

        return servicioMapper.aResponse(servicioRepository.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listarPorSalon(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(servicioMapper::aResponse)
                .toList();
    }

    @Transactional
    public ServicioResponse actualizar(Long salonId, Long servicioId, ServicioRequest request) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        if (request.activo() != null) {
            servicio.setActivo(request.activo());
        }

        return servicioMapper.aResponse(servicioRepository.save(servicio));
    }
}