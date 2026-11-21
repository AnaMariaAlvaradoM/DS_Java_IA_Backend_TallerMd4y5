
package com.devsenior.sillon.servicio;

import com.devsenior.sillon.salon.Salon;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

    public Servicio aEntidad(ServicioRequest request, Salon salon) {

        Servicio servicio = new Servicio();

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setPrecio(request.getPrecio());
        servicio.setActivo(
            request.getActivo() == null || request.getActivo()
        );
        servicio.setSalon(salon);

        return servicio;
    }

    public ServicioResponse aRespuesta(Servicio servicio) {

        ServicioResponse response = new ServicioResponse();

        response.setId(servicio.getId());
        response.setNombre(servicio.getNombre());
        response.setDescripcion(servicio.getDescripcion());
        response.setDuracionMinutos(servicio.getDuracionMinutos());
        response.setPrecio(servicio.getPrecio());
        response.setActivo(servicio.isActivo());

        return response;
    }

    public void actualizarEntidad(
            ServicioRequest request,
            Servicio servicio) {

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setPrecio(request.getPrecio());
        servicio.setActivo(
            request.getActivo() == null || request.getActivo()
        );
    }
}
