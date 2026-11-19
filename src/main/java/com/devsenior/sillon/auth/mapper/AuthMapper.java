package com.devsenior.sillon.auth.mapper;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.usuario.modelo.Usuario;

@Component
public class AuthMapper {

    public RespuestaAuth aRespuesta(
            Usuario usuario,
            String token,
            int expiraEnMinutos) {

        return new RespuestaAuth(
                token,
                "Bearer",
                expiraEnMinutos,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getSalon().getId(),
                usuario.getSalon().getSlug());
    }

}