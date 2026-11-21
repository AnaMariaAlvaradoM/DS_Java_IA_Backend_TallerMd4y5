
package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

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
    public ServicioResponse crear(
            Long salonId,
            ServicioRequest request) {

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        Servicio servicio = servicioMapper.aEntidad(request, salon);

        Servicio guardado = servicioRepository.save(servicio);

        return servicioMapper.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long salonId) {

        return servicioRepository.findBySalonId(salonId)
                .stream()
                .map(servicioMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public ServicioResponse actualizar(
            Long salonId,
            Long servicioId,
            ServicioRequest request) {

        Servicio servicio = servicioRepository
                .findByIdAndSalonId(servicioId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Servicio no encontrado"
                ));

        servicioMapper.actualizarEntidad(request, servicio);

        Servicio actualizado = servicioRepository.save(servicio);

        return servicioMapper.aRespuesta(actualizado);
    }
}
