package com.devsenior.sillon.servicio.mapper;

import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.dto.ServicioResponse;

public class ServicioMapper {
    public static ServicioResponse toServicioResponse(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.getActivo()
        );
    }
}
