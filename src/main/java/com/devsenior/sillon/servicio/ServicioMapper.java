package com.devsenior.sillon.servicio;

import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {
    public ServicioResponse convertir(Servicio servicio) {
        return new ServicioResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.isActivo());
    }

    public void aplicar(ServicioRequest request, Servicio servicio) {
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo());
    }
}
