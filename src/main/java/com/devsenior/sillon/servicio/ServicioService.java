package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;

/** Catálogo de servicios del salón (paso 3.1). Todo se filtra por el salón del usuario autenticado. */
@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final SalonRepository salonRepository;
    private final ServicioMapper mapper;

    public ServicioService(ServicioRepository servicioRepository, SalonRepository salonRepository,
            ServicioMapper mapper) {
        this.servicioRepository = servicioRepository;
        this.salonRepository = salonRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ServicioResponse crear(Long salonId, ServicioRequest peticion) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El salón no existe"));
        Servicio servicio = servicioRepository.save(mapper.aEntidad(peticion, salon));
        return mapper.aRespuesta(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long salonId) {
        return servicioRepository.findAllBySalonId(salonId).stream()
                .map(mapper::aRespuesta)
                .toList();
    }

    @Transactional
    public ServicioResponse actualizar(Long salonId, Long servicioId, ServicioRequest peticion) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El servicio no existe"));
        mapper.actualizar(servicio, peticion);
        servicioRepository.save(servicio);
        return mapper.aRespuesta(servicio);
    }
}
