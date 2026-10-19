package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioService {
    private final ServicioRepository servicios;
    private final SalonRepository salones;
    private final ServicioMapper mapper;

    public ServicioService(ServicioRepository servicios, SalonRepository salones, ServicioMapper mapper) {
        this.servicios = servicios;
        this.salones = salones;
        this.mapper = mapper;
    }

    @Transactional
    public ServicioResponse crear(Long salonId, ServicioRequest request) {
        Servicio servicio = new Servicio();
        servicio.setSalon(salones.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado")));
        mapper.aplicar(request, servicio);
        return mapper.convertir(servicios.saveAndFlush(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long salonId) {
        return servicios.findAllBySalonId(salonId).stream().map(mapper::convertir).toList();
    }

    @Transactional
    public ServicioResponse editar(Long salonId, Long id, ServicioRequest request) {
        Servicio servicio = servicios.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        mapper.aplicar(request, servicio);
        return mapper.convertir(servicios.saveAndFlush(servicio));
    }
}
