package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class CatalogoServicio {

    private final ServicioRepository servicioRepository;
    private final ServicioMapper servicioMapper;
    private final SalonRepository salonRepository;

    public CatalogoServicio(ServicioRepository servicioRepository, ServicioMapper servicioMapper,
            SalonRepository salonRepository) {
        this.servicioRepository = servicioRepository;
        this.servicioMapper = servicioMapper;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public ServicioRespuesta crear(Long salonId, ServicioSolicitud solicitud) {
        Servicio servicio = new Servicio();
        servicio.setSalon(cargarSalon(salonId));
        llenar(servicio, solicitud);
        servicio = servicioRepository.save(servicio);
        return servicioMapper.aRespuesta(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(Long salonId) {
        return servicioRepository.findBySalonId(salonId).stream()
                .map(servicioMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public ServicioRespuesta actualizar(Long salonId, Long id, ServicioSolicitud solicitud) {
        Servicio servicio = servicioRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        boolean activoAnterior = servicio.isActivo();
        llenar(servicio, solicitud);
        if (solicitud.activo() == null) {
            servicio.setActivo(activoAnterior);
        }
        servicio = servicioRepository.save(servicio);
        return servicioMapper.aRespuesta(servicio);
    }

    private void llenar(Servicio servicio, ServicioSolicitud solicitud) {
        servicio.setNombre(solicitud.nombre().trim());
        servicio.setDescripcion(solicitud.descripcion() == null ? null : solicitud.descripcion().trim());
        servicio.setDuracionMinutos(solicitud.duracionMinutos());
        servicio.setPrecio(solicitud.precio());
        servicio.setActivo(solicitud.activo() == null || solicitud.activo());
    }

    private Salon cargarSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
    }
}
