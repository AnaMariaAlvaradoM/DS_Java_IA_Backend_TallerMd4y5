package com.devsenior.sillon.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Reglas de acceso (paso 2.4): sesión sin estado, CSRF deshabilitado,
 * rutas públicas del contrato y la matriz de roles y permisos de la
 * sección 2 del PDF, declarada completa aunque algunas rutas lleguen
 * en fases posteriores.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private final FiltroJwt filtroJwt;
    private final PuntoEntradaAutenticacion puntoEntrada;
    private final ManejadorAccesoDenegado accesoDenegado;

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaAutenticacion puntoEntrada,
            ManejadorAccesoDenegado accesoDenegado) {
        this.filtroJwt = filtroJwt;
        this.puntoEntrada = puntoEntrada;
        this.accesoDenegado = accesoDenegado;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(accesoDenegado))
                .authorizeHttpRequests(auth -> auth
                        // Públicas: autenticación, rutas públicas, webhook y resultado del pago, Swagger y /error
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resultado").permitAll()
                        // Compartidas entre DUENO y ESTILISTA
                        .requestMatchers(HttpMethod.GET, "/api/servicios").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole("DUENO", "ESTILISTA")
                        // Solo ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Solo DUENO
                        .requestMatchers("/api/salon/**", "/api/servicios/**", "/api/estilistas/**",
                                "/api/clientes/**", "/api/citas/**", "/api/solicitudes/**", "/api/pagos/**")
                        .hasRole("DUENO")
                        .anyRequest().authenticated())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
