package com.devsenior.sillon.servicio;

public record ServicioRespuesta(Long id, String nombre, String descripcion,
        Integer duracionMinutos, Integer precio, Boolean activo) {
    public static ServicioRespuesta desde(Servicio servicio) {
        return new ServicioRespuesta(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.getActivo());
    }
}
