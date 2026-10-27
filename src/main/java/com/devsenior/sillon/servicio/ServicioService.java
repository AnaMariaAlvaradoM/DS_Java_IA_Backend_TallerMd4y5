package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class ServicioService {
    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;

    public ServicioService(ServicioRepository servicioRepository, SalonRepository salonRepository) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
    }
    @Transactional
    public ServicioResponse crearServicio(ServicioRequest request,Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        Servicio servicio = new Servicio();
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        if (request.activo() != null) {
            servicio.setActivo(request.activo());
        }
        servicio.setSalon(salon);
        servicioRepository.save(servicio);
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.getActivo()
        );
    }
    @Transactional
    public List<ServicioResponse> obtenerServicio( Long salonId) {
       return servicioRepository.findBySalonId(salonId).stream()
                .map(ServicioMapper::toServicioResponse)
               .collect(Collectors.toList());

    }
    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest request, Long salonId) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado con ID: " + id));
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo());
        servicio.setSalon(salon);
        Servicio actualizado = servicioRepository.save(servicio);
        return ServicioMapper.toServicioResponse(actualizado);
    }
}
