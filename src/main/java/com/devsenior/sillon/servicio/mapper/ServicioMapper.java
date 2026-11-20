package com.devsenior.sillon.servicio.mapper;

import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.modelo.Servicio;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

    public ServicioResponse aResponse(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.isActivo());
    }
}