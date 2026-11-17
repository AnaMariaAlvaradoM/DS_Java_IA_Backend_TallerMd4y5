package com.devSenior.sillon.seguridad;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {

	@Bean
	public SecurityFilterChain filtroSeguridad(HttpSecurity http, FiltroJwt filtroJwt,
			EntradaAutenticacion entradaAutenticacion, ManejadorAccesoDenegado manejadorAccesoDenegado) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(errores -> errores
						.authenticationEntryPoint(entradaAutenticacion)
						.accessDeniedHandler(manejadorAccesoDenegado))
				.authorizeHttpRequests(acceso -> acceso
						.requestMatchers("/api/auth/**", "/api/publico/**", "/api/pagos/webhook",
								"/api/pagos/resultado", "/error").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
								.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/usuarios/yo").authenticated()
						.requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("DUENO")
						.requestMatchers(HttpMethod.GET, "/api/servicios").hasAnyRole("DUENO", "ESTILISTA")
						.requestMatchers(HttpMethod.POST, "/api/servicios").hasRole("DUENO")
						.requestMatchers(HttpMethod.PUT, "/api/servicios/**").hasRole("DUENO")
						.requestMatchers("/api/estilistas", "/api/estilistas/**", "/api/clientes", "/api/clientes/**")
								.hasRole("DUENO")
						.requestMatchers(HttpMethod.POST, "/api/citas/*/recordatorio").hasRole("DUENO")
						.requestMatchers(HttpMethod.POST, "/api/citas").hasRole("DUENO")
						.requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
						.requestMatchers(HttpMethod.GET, "/api/citas/*").hasAnyRole("DUENO", "ESTILISTA")
						.requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole("DUENO", "ESTILISTA")
						.requestMatchers("/api/solicitudes", "/api/solicitudes/**").hasRole("DUENO")
						.requestMatchers(HttpMethod.POST, "/api/pagos/suscripcion", "/api/pagos/creditos").hasRole("DUENO")
						.requestMatchers("/api/salon", "/api/salon/**").hasRole("DUENO")
						.anyRequest().authenticated())
				.addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public FilterRegistrationBean<FiltroJwt> registroFiltroJwt(
			FiltroJwt filtroJwt) {

		FilterRegistrationBean<FiltroJwt> registro =
				new FilterRegistrationBean<>(filtroJwt);

		registro.setEnabled(false);

		return registro;
	}

	@Bean
	public FiltroJwt filtroJwt(
			JwtServicio jwtServicio,
			UsuarioPrincipalServicio usuarioPrincipalServicio) {

		return new FiltroJwt(jwtServicio, usuarioPrincipalServicio);
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
