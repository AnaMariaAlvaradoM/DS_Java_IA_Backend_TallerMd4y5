package com.devsenior.sillon.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Reglas de acceso: tabla "Roles y permisos" de la seccion 2 del contrato.
 * Spring evalua las reglas en el orden en que se escriben y gana la primera que coincide:
 * por eso las reglas especificas (por metodo HTTP) van antes que las generales.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String DUENO = "DUENO";
    private static final String ESTILISTA = "ESTILISTA";
    private static final String ADMIN = "ADMIN";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtServicio jwtServicio,
            UserDetailsService detallesServicio, PuntoEntradaJwt puntoEntrada,
            AccesoDenegadoJwt accesoDenegado) throws Exception {

        http
                // API sin sesion ni cookies: no hay nada que proteger con CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 y 403 con el formato unico de error.
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(accesoDenegado))
                .authorizeHttpRequests(reglas -> reglas
                        // ---- Publicas. /error DEBE ser publica: si no, un fallo del servidor se ve como 401. ----
                        .requestMatchers("/api/auth/**", "/api/publico/**",
                                "/api/pagos/webhook", "/api/pagos/resultado",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/error").permitAll()

                        // ---- Solo el dueno ----
                        .requestMatchers("/api/salon", "/api/salon/**").hasRole(DUENO)
                        .requestMatchers("/api/estilistas", "/api/estilistas/**").hasRole(DUENO)
                        .requestMatchers("/api/clientes", "/api/clientes/**").hasRole(DUENO)
                        .requestMatchers("/api/solicitudes", "/api/solicitudes/**").hasRole(DUENO)
                        .requestMatchers("/api/pagos/**").hasRole(DUENO)

                        // ---- Servicios: ver = dueno y estilista; crear y editar = dueno ----
                        .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/servicios/**")
                                .hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers("/api/servicios", "/api/servicios/**").hasRole(DUENO)

                        // ---- Citas: agenda y cambio de estado = dueno y estilista (el servicio afina por estilista);
                        //      crear cita y recordatorio con IA = dueno ----
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers("/api/citas", "/api/citas/**").hasRole(DUENO)

                        // ---- Solo el administrador ----
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)

                        // Cualquier otra ruta pide estar autenticado.
                        .anyRequest().authenticated())
                // El filtro JWT corre antes del filtro de usuario y contrasena de Spring.
                .addFilterBefore(new JwtFiltro(jwtServicio, detallesServicio),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
