
package com.devsenior.sillon.usuario;

import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.servicio.Servicio;

@Component
public class EstilistaMapper {

    public EstilistaResponse aRespuesta(Usuario usuario) {

        EstilistaResponse response = new EstilistaResponse();

        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setActivo(usuario.isActivo());

        List<String> servicios = usuario.getServicios()
                .stream()
                .map(Servicio::getNombre)
                .sorted()
                .toList();

        response.setServicios(servicios);

        return response;
    }
}
