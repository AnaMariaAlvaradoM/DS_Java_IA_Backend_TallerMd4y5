package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
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
    public ServicioRespuesta crear(Long salonId, ServicioPeticion peticion) {
        Servicio servicio = new Servicio();
        ServicioMapper.copiar(peticion, servicio);
        servicio.setSalon(salonRepository.getReferenceById(salonId));
        return ServicioMapper.aRespuesta(servicioRepository.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(Long salonId) {
        return servicioRepository.findAllBySalonIdOrderByIdAsc(salonId).stream()
                .map(ServicioMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public ServicioRespuesta editar(Long salonId, Long id, ServicioPeticion peticion) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
        ServicioMapper.copiar(peticion, servicio);
        return ServicioMapper.aRespuesta(servicioRepository.save(servicio));
    }
}
