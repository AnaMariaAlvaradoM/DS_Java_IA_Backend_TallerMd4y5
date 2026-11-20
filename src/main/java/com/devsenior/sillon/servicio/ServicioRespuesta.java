package com.devsenior.sillon.servicio;

public record ServicioRespuesta(Long id, String nombre, String descripcion,
                                Integer duracionMinutos, Integer precio, boolean activo) {
}
