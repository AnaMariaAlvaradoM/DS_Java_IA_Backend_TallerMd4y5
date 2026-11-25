package com.devsenior.sillon.servicio;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.salon.Salon;

/** Mapper propio entre la entidad Servicio y sus DTOs. */
@Component
public class ServicioMapper {

    public Servicio aEntidad(ServicioCreacionPeticion peticion, Salon salon) {
        Servicio servicio = new Servicio();
        servicio.setNombre(peticion.nombre().trim());
        servicio.setDescripcion(peticion.descripcion() != null ? peticion.descripcion().trim() : null);
        servicio.setDuracionMinutos(peticion.duracionMinutos());
        servicio.setPrecio(peticion.precio());
        servicio.setActivo(peticion.activo() == null || peticion.activo());
        servicio.setSalon(salon);
        return servicio;
    }

    public ServicioRespuesta aRespuesta(Servicio servicio) {
        return new ServicioRespuesta(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.isActivo());
    }

    public void actualizar(Servicio servicio, ServicioCreacionPeticion peticion) {
        servicio.setNombre(peticion.nombre().trim());
        servicio.setDescripcion(peticion.descripcion() != null ? peticion.descripcion().trim() : null);
        servicio.setDuracionMinutos(peticion.duracionMinutos());
        servicio.setPrecio(peticion.precio());
        if (peticion.activo() != null) {
            servicio.setActivo(peticion.activo());
        }
    }
}
