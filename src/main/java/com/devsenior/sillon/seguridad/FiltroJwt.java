
package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
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
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String cabecera = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (cabecera != null
                && cabecera.startsWith("Bearer ")
                && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            String token = cabecera.substring(7).trim();

            if (!token.isBlank()) {
                try {
                    String email = servicioJwt.obtenerEmail(token);

                    UserDetails detalles =
                            servicioDetallesUsuario.loadUserByUsername(email);

                    if (detalles instanceof UsuarioPrincipal principal
                            && servicioJwt.validarToken(token, principal)) {

                        UsernamePasswordAuthenticationToken autenticacion =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                );

                        autenticacion.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(autenticacion);
                    }

                } catch (JwtException
                        | IllegalArgumentException
                        | UsernameNotFoundException e) {

                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
