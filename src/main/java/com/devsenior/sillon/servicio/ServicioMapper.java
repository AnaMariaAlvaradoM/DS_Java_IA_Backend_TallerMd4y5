package com.devsenior.sillon.servicio;

import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

    public ServicioRespuesta aRespuesta(Servicio servicio) {
        return new ServicioRespuesta(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.isActivo());
    }
}
