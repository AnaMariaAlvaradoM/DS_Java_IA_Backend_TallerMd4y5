package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtServicio jwtServicio;
    private final UsuarioRepository usuarioRepository;

    public FiltroJwt(JwtServicio jwtServicio, UsuarioRepository usuarioRepository) {
        this.jwtServicio = jwtServicio;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        String cabecera = solicitud.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)) {
            String token = cabecera.substring(PREFIJO_BEARER.length());
            try {
                Claims reclamos = jwtServicio.validar(token);
                String email = reclamos.getSubject();
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    usuarioRepository.findByEmail(email).filter(Usuario::isActivo).ifPresent(usuario -> {
                        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
                        UsernamePasswordAuthenticationToken autenticacion =
                                new UsernamePasswordAuthenticationToken(principal, null,
                                        principal.getAuthorities());
                        autenticacion.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(solicitud));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
                }
            } catch (RuntimeException excepcion) {
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
