package com.devsenior.sillon.seguridad;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroJwt extends OncePerRequestFilter {
    private final ServicioJwt jwt;
    private final ServicioUsuariosSeguridad usuarios;

    public FiltroJwt(ServicioJwt jwt, ServicioUsuariosSeguridad usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        String autorizacion = solicitud.getHeader("Authorization");
        if (autorizacion != null && autorizacion.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var claims = jwt.validar(autorizacion.substring(7));
                UsuarioPrincipal usuario = (UsuarioPrincipal) usuarios.loadUserByUsername(claims.getSubject());
                Object salonClaim = claims.get("salonId");
                if (usuario.isEnabled() && usuario.getRol().name().equals(claims.get("rol", String.class))
                        && salonClaim instanceof Number salonId
                        && usuario.getSalonId().equals(salonId.longValue())) {
                    var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(solicitud));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException | ClassCastException excepcion) {
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
