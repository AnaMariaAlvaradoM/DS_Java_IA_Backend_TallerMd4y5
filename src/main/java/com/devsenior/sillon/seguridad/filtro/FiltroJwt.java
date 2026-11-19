package com.devsenior.sillon.seguridad.filtro;

import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import com.devsenior.sillon.seguridad.service.JwtService;
import com.devsenior.sillon.seguridad.service.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee "Authorization: Bearer <token>", lo valida y deja al usuario autenticado.
 * Si el token falta o es inválido, la petición sigue sin autenticar (y las reglas devuelven 401).
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public FiltroJwt(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            jwtService.correoSiEsValido(token).ifPresent(correo -> autenticar(correo, request));
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(String correo, HttpServletRequest request) {
        try {
            UsuarioPrincipal usuario = usuarioDetailsService.loadUserByUsername(correo);
            if (!usuario.isEnabled()) {
                return;
            }
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (UsernameNotFoundException ex) {
            // El usuario del token ya no existe: la petición sigue sin autenticar
        }
    }
}
