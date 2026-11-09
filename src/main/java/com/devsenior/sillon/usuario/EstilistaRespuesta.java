package com.devsenior.sillon.usuario;

import com.devsenior.sillon.servicio.Servicio;
import java.util.List;

public record EstilistaRespuesta(Long id, String nombre, String email, Boolean activo, List<String> servicios) {
    public static EstilistaRespuesta desde(Usuario estilista) {
        return new EstilistaRespuesta(estilista.getId(), estilista.getNombre(), estilista.getEmail(),
                estilista.getActivo(), estilista.getServicios().stream()
                        .sorted((a, b) -> a.getId().compareTo(b.getId()))
                        .map(Servicio::getNombre).toList());
    }
}
