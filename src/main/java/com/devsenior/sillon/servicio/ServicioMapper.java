package com.devsenior.sillon.servicio;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;

/** Mapper propio, sin Lombok ni librerías de mapeo (paso 3.1). */
@Component
public class ServicioMapper {

    public Servicio aEntidad(ServicioRequest peticion, Salon salon) {
        Servicio servicio = new Servicio();
        servicio.setSalon(salon);
        copiarCampos(servicio, peticion);
        return servicio;
    }

    public void actualizar(Servicio servicio, ServicioRequest peticion) {
        copiarCampos(servicio, peticion);
    }

    public ServicioResponse aRespuesta(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.isActivo());
    }

    private void copiarCampos(Servicio servicio, ServicioRequest peticion) {
        servicio.setNombre(peticion.nombre());
        servicio.setDescripcion(peticion.descripcion());
        servicio.setDuracionMinutos(peticion.duracionMinutos());
        servicio.setPrecio(peticion.precio());
        servicio.setActivo(peticion.activoODefecto());
    }
}
