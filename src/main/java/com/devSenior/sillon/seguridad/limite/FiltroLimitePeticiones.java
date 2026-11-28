package com.devSenior.sillon.seguridad.limite;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.devSenior.sillon.compartido.dto.ErrorRespuesta;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroLimitePeticiones extends org.springframework.web.filter.OncePerRequestFilter {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final Clock reloj;
	private final VentanaLimite ventana;
	private final ResolvedorIp resolvedorIp;

	public FiltroLimitePeticiones(Clock reloj, @Value("${sillon.proxy.direcciones:}") String direccionesProxy) {
		this.reloj = reloj;
		this.ventana = new VentanaLimite(30, Duration.ofMinutes(1));
		this.resolvedorIp = new ResolvedorIp(direccionesProxy);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (!aplica(peticion)) {
			cadena.doFilter(peticion, respuesta);
			return;
		}
		if (ventana.permitir(resolvedorIp.ip(peticion), reloj.instant())) {
			cadena.doFilter(peticion, respuesta);
			return;
		}
		respuesta.setStatus(429);
		respuesta.setContentType("application/json");
		respuesta.setCharacterEncoding("UTF-8");
		ErrorRespuesta cuerpo = new ErrorRespuesta(429, "Too Many Requests",
				"Demasiadas peticiones. El límite es 30 por minuto.",
				LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
		respuesta.getWriter().write(JSON.writeValueAsString(cuerpo));
	}

	private boolean aplica(HttpServletRequest peticion) {
		String ruta = ruta(peticion);
		if (ruta.startsWith("/api/publico/")) {
			return true;
		}
		return "/api/auth/login".equals(ruta) && "POST".equalsIgnoreCase(peticion.getMethod());
	}

	private String ruta(HttpServletRequest peticion) {
		String uri = peticion.getRequestURI();
		String contexto = peticion.getContextPath();
		if (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) {
			return uri.substring(contexto.length());
		}
		return uri;
	}

}
