
package com.devsenior.sillon.usuario;

import com.devsenior.sillon.catalogo.ServicioRespuesta;

import java.util.List;

public record EstilistaRespuesta(
        Long id,
        String nombre,
        String email,
        Rol rol,
        Boolean activo,
        List<ServicioRespuesta> servicios
) {
    public static EstilistaRespuesta desdeEntidad(Usuario usuario) {
        List<ServicioRespuesta> servicios = usuario.getServicios() == null
                ? List.of()
                : usuario.getServicios().stream()
                .map(ServicioRespuesta::desdeEntidad)
                .toList();

        return new EstilistaRespuesta(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                servicios
        );
    }
}