package com.devsenior.sillon.catalogo;

import java.math.BigDecimal;

public record ServicioRespuesta(
        Long id,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        BigDecimal precio,
        Boolean activo
) {
    public static ServicioRespuesta desdeEntidad(Servicio servicio) {
        return new ServicioRespuesta(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio(),
                servicio.getActivo()
        );
    }
}