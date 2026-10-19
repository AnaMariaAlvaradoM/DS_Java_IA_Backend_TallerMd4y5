package com.devsenior.sillon.usuario;

import com.devsenior.sillon.servicio.Servicio;
import org.springframework.stereotype.Component;

@Component
public class EstilistaMapper {
    public EstilistaResponse convertir(Usuario usuario) {
        return new EstilistaResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.isActivo(),
                usuario.getServicios().stream().map(Servicio::getNombre).toList());
    }
}
