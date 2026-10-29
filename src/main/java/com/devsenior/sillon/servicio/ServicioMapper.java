package com.devsenior.sillon.servicio;

public class ServicioMapper {

    private ServicioMapper() {
    }

    public static ServicioRespuesta aRespuesta(Servicio servicio) {
        return new ServicioRespuesta(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.isActivo());
    }
}
