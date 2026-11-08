package com.devsenior.sillon.publico;

import com.devsenior.sillon.usuario.Usuario;
import java.util.List;

public record EstilistaPublicoRespuesta(Long id, String nombre, List<Long> servicioIds) {
    public static EstilistaPublicoRespuesta desde(Usuario estilista) {
        return new EstilistaPublicoRespuesta(estilista.getId(), estilista.getNombre(),
                estilista.getServicios().stream()
                        .filter(servicio -> Boolean.TRUE.equals(servicio.getActivo()))
                        .map(servicio -> servicio.getId()).sorted().toList());
    }
}
