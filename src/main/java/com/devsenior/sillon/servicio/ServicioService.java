package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequestDto;
import com.devsenior.sillon.servicio.dto.ServicioResponseDto;

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
    public ServicioResponseDto crearServicio(Long salonId, ServicioRequestDto request) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salon no encontrado"));

        ServicioSalon servicio = new ServicioSalon();
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setPrecio(request.precio());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setActivo(true);
        servicio.setSalon(salon);

        servicio = servicioRepository.save(servicio);
        return mapearADto(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponseDto> listarServicios(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional
    public ServicioResponseDto actualizarServicio(Long id, Long salonId, ServicioRequestDto request) {
        ServicioSalon servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado en su salon"));

        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setPrecio(request.precio());
        servicio.setDuracionMinutos(request.duracionMinutos());

        servicio = servicioRepository.save(servicio);
        return mapearADto(servicio);
    }

    @Transactional
    public void cambiarEstado(Long id, Long salonId, boolean activo) {
        ServicioSalon servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado en su salon"));

        servicio.setActivo(activo);
        servicioRepository.save(servicio);
    }

    private ServicioResponseDto mapearADto(ServicioSalon servicio) {
        return new ServicioResponseDto(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.isActivo()
        );
    }
}