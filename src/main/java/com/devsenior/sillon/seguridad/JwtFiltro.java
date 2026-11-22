package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee la cabecera "Authorization: Bearer <token>", valida el token, carga el usuario y lo deja autenticado.
 * Si el token falta o es invalido, NO corta la peticion: la deja pasar sin autenticar y la cadena de
 * seguridad responde 401 (en rutas protegidas) o la atiende normal (en rutas publicas).
 *
 * Esta clase no es un @Component a proposito: un Filter registrado como bean lo instala Spring Boot
 * ademas de la cadena de seguridad y correria dos veces. Se crea dentro de ConfiguracionSeguridad.
 */
public class JwtFiltro extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtServicio jwtServicio;
    private final UserDetailsService detallesServicio;

    public JwtFiltro(JwtServicio jwtServicio, UserDetailsService detallesServicio) {
        this.jwtServicio = jwtServicio;
        this.detallesServicio = detallesServicio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
            jwtServicio.extraerEmail(token).ifPresent(email -> autenticar(email, request));
        }
        cadena.doFilter(request, response);
    }

    private void autenticar(String email, HttpServletRequest request) {
        try {
            UserDetails usuario = detallesServicio.loadUserByUsername(email);
            if (!usuario.isEnabled()) {
                return; // cuenta desactivada: el token sigue firmado, pero ya no sirve
            }
            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (UsernameNotFoundException e) {
            // El usuario ya no existe: se queda sin autenticar y el resultado sera 401.
        }
    }
}
