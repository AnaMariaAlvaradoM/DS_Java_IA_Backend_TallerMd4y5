package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ServicioServicio {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;
    private final ServicioMapper servicioMapper;

    public ServicioServicio(ServicioRepository servicioRepository,
                            SalonRepository salonRepository,
                            ServicioMapper servicioMapper) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
        this.servicioMapper = servicioMapper;
    }

    @Transactional
    public ServicioRespuesta crear(Long salonId, ServicioCreacionPeticion peticion) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Servicio servicio = servicioMapper.aEntidad(peticion, salon);
        Servicio guardado = servicioRepository.save(servicio);
        return servicioMapper.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(servicioMapper::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioRespuesta obtener(Long salonId, Long id) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado o no pertenece a su salón"));
        return servicioMapper.aRespuesta(servicio);
    }

    @Transactional
    public ServicioRespuesta actualizar(Long salonId, Long id, ServicioCreacionPeticion peticion) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado o no pertenece a su salón"));

        servicioMapper.actualizar(servicio, peticion);
        Servicio guardado = servicioRepository.save(servicio);
        return servicioMapper.aRespuesta(guardado);
    }
}
