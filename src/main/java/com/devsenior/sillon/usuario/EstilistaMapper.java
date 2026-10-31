package com.devsenior.sillon.usuario;

import com.devsenior.sillon.servicio.Servicio;

public class EstilistaMapper {

    private EstilistaMapper() {
    }

    public static EstilistaRespuesta aRespuesta(Usuario usuario) {
        return new EstilistaRespuesta(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.isActivo(),
                usuario.getServicios().stream().map(Servicio::getNombre).toList());
    }
}
