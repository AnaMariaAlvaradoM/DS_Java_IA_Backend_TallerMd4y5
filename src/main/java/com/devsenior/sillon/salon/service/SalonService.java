package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.ConfiguracionRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.mapper.SalonMapper;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El salonId siempre llega del usuario autenticado, nunca de la URL ni del cuerpo. */
@Service
public class SalonService {

    private final SalonRepository salonRepository;
    private final SalonMapper salonMapper;

    public SalonService(SalonRepository salonRepository, SalonMapper salonMapper) {
        this.salonRepository = salonRepository;
        this.salonMapper = salonMapper;
    }

    @Transactional(readOnly = true)
    public SalonResponse obtener(Long salonId) {
        return salonMapper.toResponse(obtenerEntidad(salonId));
    }

    @Transactional
    public SalonResponse actualizarConfiguracion(Long salonId, ConfiguracionRequest request) {
        if (!request.horaApertura().isBefore(request.horaCierre())) {
            throw new ReglaNegocioException("La hora de apertura debe ser anterior a la hora de cierre");
        }
        Salon salon = obtenerEntidad(salonId);
        salon.setConfiguracion(new ConfiguracionSalon(request.horaApertura(), request.horaCierre(),
                request.abreDomingos(), request.intervaloMinutos()));
        return salonMapper.toResponse(salonRepository.save(salon));
    }

    /** Solo para ADMIN: todos los salones de la plataforma. */
    @Transactional(readOnly = true)
    public List<SalonResponse> listarTodos() {
        return salonRepository.findAll().stream().map(salonMapper::toResponse).toList();
    }

    /** Para las rutas públicas: el salón se identifica por su slug (404 si no existe). */
    @Transactional(readOnly = true)
    public Salon obtenerPorSlug(String slug) {
        return salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el salón " + slug));
    }

    @Transactional(readOnly = true)
    public Salon obtenerEntidad(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el salón"));
    }
}
