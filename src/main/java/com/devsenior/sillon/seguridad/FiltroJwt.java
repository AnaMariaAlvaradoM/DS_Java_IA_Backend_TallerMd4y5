package com.devsenior.sillon.seguridad;

import java.io.IOException;

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

/**
 * Lee la cabecera Authorization, valida el token y deja al usuario
 * autenticado. Si el token es inválido, sigue sin autenticar: el 401 lo
 * produce el punto de entrada (paso 2.2).
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;
    private final DetallesUsuarioService detallesUsuarioService;

    public FiltroJwt(ServicioJwt servicioJwt, DetallesUsuarioService detallesUsuarioService) {
        this.servicioJwt = servicioJwt;
        this.detallesUsuarioService = detallesUsuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = servicioJwt.extraerEmail(cabecera.substring(PREFIJO.length()));
                UserDetails usuario = detallesUsuarioService.loadUserByUsername(email);
                if (usuario.isEnabled()) {
                    UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | UsernameNotFoundException ex) {
                // Token inválido o usuario inexistente: la petición sigue sin autenticar
            }
        }
        filterChain.doFilter(request, response);
    }
}
