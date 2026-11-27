package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FiltroJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;
    private final ServicioDetallesUsuario servicioDetallesUsuario;

    public FiltroJwt(
            ServicioJwt servicioJwt,
            ServicioDetallesUsuario servicioDetallesUsuario) {

        this.servicioJwt = servicioJwt;
        this.servicioDetallesUsuario = servicioDetallesUsuario;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            FilterChain cadena)
            throws ServletException, IOException {

        String cabecera = solicitud.getHeader("Authorization");

        if (cabecera != null
                && cabecera.startsWith("Bearer ")
                && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            try {
                String token = cabecera.substring(7);
                Claims datos = servicioJwt.validarToken(token);
                String email = datos.getSubject();

                if (email != null && !email.isBlank()) {

                    UsuarioPrincipal usuario =
                            servicioDetallesUsuario.loadUserByUsername(email);

                    if (usuario.isEnabled()) {

                        var autenticacion =
                                new UsernamePasswordAuthenticationToken(
                                        usuario,
                                        null,
                                        usuario.getAuthorities());

                        autenticacion.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(solicitud));

                        SecurityContextHolder.getContext()
                                .setAuthentication(autenticacion);
                    }
                }

            } catch (JwtException | IllegalArgumentException
                    | UsernameNotFoundException excepcion) {

                SecurityContextHolder.clearContext();
            }
        }

        cadena.doFilter(solicitud, respuesta);
    }

}