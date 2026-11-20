package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.modelo.Usuario;

import java.util.List;

public record EstilistaPublicoDto(
        Long id,
        String nombre,
        List<Long> servicioIds) {
    public static EstilistaPublicoDto desde(Usuario estilista) {
        List<Long> ids = estilista.getServicios().stream()
                .filter(Servicio::isActivo)
                .map(Servicio::getId)
                .sorted()
                .toList();
        return new EstilistaPublicoDto(
                estilista.getId(),
                estilista.getNombre(),
                ids);
    }
}