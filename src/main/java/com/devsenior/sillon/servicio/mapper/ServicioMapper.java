package com.devsenior.sillon.servicio.mapper;

import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.model.Servicio;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

    public ServicioResponse toResponse(Servicio servicio) {
        return new ServicioResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.isActivo());
    }

    /** Copia los campos editables del request a la entidad (sirve para crear y para actualizar). */
    public void copiar(ServicioRequest request, Servicio servicio) {
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() == null ? null : request.descripcion().trim());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo() == null || request.activo());
    }
}
