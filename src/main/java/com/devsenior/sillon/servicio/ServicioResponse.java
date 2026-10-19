package com.devsenior.sillon.servicio;

public record ServicioResponse(Long id, String nombre, String descripcion, int duracionMinutos,
                               long precio, boolean activo) {
}
