package com.devsenior.sillon.seguridad.config;

import com.devsenior.sillon.seguridad.filtro.FiltroJwt;
import com.devsenior.sillon.seguridad.handler.EscritorErroresSeguridad;
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

/** Reglas de acceso: exactamente la tabla de roles y permisos de la SPEC. */
@Configuration
public class ConfiguracionSeguridad {

    private static final String DUENO = "DUENO";
    private static final String ESTILISTA = "ESTILISTA";
    private static final String ADMIN = "ADMIN";

    private final FiltroJwt filtroJwt;
    private final EscritorErroresSeguridad escritorErrores;

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, EscritorErroresSeguridad escritorErrores) {
        this.filtroJwt = filtroJwt;
        this.escritorErrores = escritorErrores;
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(escritorErrores)
                        .accessDeniedHandler(escritorErrores))
                .authorizeHttpRequests(reglas -> reglas
                        // Públicas
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/publico/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resultado").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Administración de la plataforma
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)
                        // Mi salón
                        .requestMatchers("/api/salon/**", "/api/salon").hasRole(DUENO)
                        // Servicios: el estilista solo los ve
                        .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/servicios/**").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers("/api/servicios", "/api/servicios/**").hasRole(DUENO)
                        // Equipo y clientes
                        .requestMatchers("/api/estilistas", "/api/estilistas/**").hasRole(DUENO)
                        .requestMatchers("/api/clientes", "/api/clientes/**").hasRole(DUENO)
                        // Agenda
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers(HttpMethod.POST, "/api/citas", "/api/citas/*/recordatorio").hasRole(DUENO)
                        // Pagos y solicitudes del chatbot
                        .requestMatchers("/api/pagos/**").hasRole(DUENO)
                        .requestMatchers("/api/solicitudes", "/api/solicitudes/**").hasRole(DUENO)
                        .anyRequest().authenticated())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
