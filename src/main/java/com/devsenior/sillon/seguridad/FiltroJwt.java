package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devsenior.sillon.usuario.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee "Authorization: Bearer ...", valida el token y deja al usuario autenticado.
 * Si el token es inválido, sigue sin autenticar: la seguridad responderá 401.
 * No es @Component a propósito: así Spring no lo registra dos veces.
 */
public class FiltroJwt extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public FiltroJwt(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String email = jwtService.extraerEmail(cabecera.substring(7).trim());
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                usuarioRepository.findByEmail(email)
                        .filter(u -> u.isActivo())
                        .map(UsuarioPrincipal::de)
                        .ifPresent(principal -> {
                            var autenticacion = new UsernamePasswordAuthenticationToken(principal, null,
                                    principal.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(autenticacion);
                        });
            }
        }
        chain.doFilter(request, response);
    }
}
