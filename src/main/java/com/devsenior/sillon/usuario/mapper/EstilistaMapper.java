package com.devsenior.sillon.usuario.mapper;

import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class EstilistaMapper {

    public EstilistaResponse toResponse(Usuario estilista) {
        return new EstilistaResponse(estilista.getId(), estilista.getNombre(), estilista.getEmail(),
                estilista.isActivo(),
                estilista.getServicios().stream().map(Servicio::getNombre).sorted().toList());
    }
}
