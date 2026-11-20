package com.devsenior.sillon.usuario.dto;

import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.modelo.Usuario;

import java.util.List;

public record EstilistaResponse(
        Long id,
        String nombre,
        String email,
        boolean activo,
        List<String> servicios
) {
    public static EstilistaResponse desde(Usuario usuario) {
        List<String> nombresServicios = usuario.getServicios().stream()
                .map(Servicio::getNombre)
                .sorted()
                .toList();
        return new EstilistaResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.isActivo(),
                nombresServicios
        );
    }
}

