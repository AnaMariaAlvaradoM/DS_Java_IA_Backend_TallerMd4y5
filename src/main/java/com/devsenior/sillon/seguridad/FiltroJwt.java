package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.usuario.Usuario;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FiltroJwt extends OncePerRequestFilter {
    private final Tokens tokens;
    private final Datos datos;

    public FiltroJwt(Tokens tokens, Datos datos) {
        this.tokens = tokens;
        this.datos = datos;
    }

    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var correo = tokens.correo(header.substring(7));
                var usuarios =
                        datos.em()
                                .createQuery(
                                        "from Usuario where email=:email and activo=true",
                                        Usuario.class)
                                .setParameter("email", correo)
                                .getResultList();
                if (!usuarios.isEmpty()) {
                    var u = usuarios.get(0);
                    var actor =
                            new Actor(u.getId(), u.getSalon().getId(), u.getRol(), u.getEmail());
                    var auth =
                            new UsernamePasswordAuthenticationToken(
                                    actor,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
