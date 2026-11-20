package com.devSenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FiltroJwt extends OncePerRequestFilter {

	private final JwtServicio jwtServicio;
	private final UsuarioPrincipalServicio usuarioPrincipalServicio;

	public FiltroJwt(JwtServicio jwtServicio, UsuarioPrincipalServicio usuarioPrincipalServicio) {
		this.jwtServicio = jwtServicio;
		this.usuarioPrincipalServicio = usuarioPrincipalServicio;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (encabezado != null && encabezado.startsWith("Bearer ")) {
			String token = encabezado.substring(7).trim();
			try {
				String correo = jwtServicio.extraerCorreo(token);
				if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UsuarioPrincipal principal = (UsuarioPrincipal) usuarioPrincipalServicio.loadUserByUsername(correo);
					if (principal.isEnabled()) {
						UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
								principal, null, principal.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(autenticacion);
					}
				}
			} catch (RuntimeException ex) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}

}
