package com.devsenior.sillon.usuario.estilista.mapper;

import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.model.Usuario;

import java.util.List;

public class EstilistaMapper {
    public static EstilistaResponse estilistaToEstilistaResponse(Usuario estilista) {
        List<String> nombresServicios = estilista.getServicios().stream()
                .map(Servicio::getNombre)
                .toList();
        return new EstilistaResponse(
                estilista.getId(),
                estilista.getNombre(),
                estilista.getEmail(),
                estilista.isActivo(),
                nombresServicios
        );
    }
}
