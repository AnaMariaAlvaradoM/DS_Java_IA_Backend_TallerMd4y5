package co.sillon;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class FiltroJwt extends OncePerRequestFilter {
    private final Tokens tokens;
    private final Datos datos;
    FiltroJwt(Tokens tokens, Datos datos) { this.tokens=tokens; this.datos=datos; }
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var header=req.getHeader("Authorization");
        if (header!=null && header.startsWith("Bearer ")) {
            try {
                var correo=tokens.correo(header.substring(7));
                var usuarios=datos.em().createQuery("from Usuario where email=:email and activo=true",Usuario.class)
                        .setParameter("email",correo).getResultList();
                if (!usuarios.isEmpty()) {
                    var u=usuarios.get(0);
                    var actor=new Actor(u.id,u.salon.id,u.rol,u.email);
                    var auth=new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.rol)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(req,res);
    }
}
