package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioServicio {
    private final ServicioRepositorio servicios;
    private final SalonRepositorio salones;

    public ServicioServicio(ServicioRepositorio servicios, SalonRepositorio salones) {
        this.servicios = servicios;
        this.salones = salones;
    }

    @Transactional
    public ServicioRespuesta crear(Long salonId, ServicioSolicitud solicitud) {
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        Servicio servicio = new Servicio();
        servicio.setSalon(salon);
        aplicar(servicio, solicitud);
        return ServicioRespuesta.desde(servicios.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioRespuesta> listar(Long salonId) {
        return servicios.findAllBySalon_IdOrderByIdAsc(salonId).stream().map(ServicioRespuesta::desde).toList();
    }

    @Transactional
    public ServicioRespuesta actualizar(Long salonId, Long id, ServicioSolicitud solicitud) {
        Servicio servicio = servicios.findByIdAndSalon_Id(id, salonId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        aplicar(servicio, solicitud);
        return ServicioRespuesta.desde(servicios.save(servicio));
    }

    private void aplicar(Servicio servicio, ServicioSolicitud solicitud) {
        servicio.setNombre(solicitud.nombre());
        servicio.setDescripcion(solicitud.descripcion());
        servicio.setDuracionMinutos(solicitud.duracionMinutos());
        servicio.setPrecio(solicitud.precio());
        servicio.setActivo(solicitud.activo() == null || solicitud.activo());
    }
}
