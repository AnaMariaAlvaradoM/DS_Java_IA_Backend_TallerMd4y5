package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFiltro jwtFiltro;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityConfig(JwtFiltro jwtFiltro, ObjectMapper objectMapper, Clock clock) {
        this.jwtFiltro = jwtFiltro;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/error").permitAll()
                        .requestMatchers("/api/pagos/webhook", "/api/pagos/resultado/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // Rutas por rol (las iremos ampliando en las siguientes fases)
                        .requestMatchers("/api/salon/**").hasRole("DUENO")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/servicios/**").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/servicios/**", "/api/estilistas/**", "/api/clientes/**").hasRole("DUENO")
                        .requestMatchers("/api/solicitudes/**", "/api/pagos/**").hasRole("DUENO")

                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> escribirError(response,
                                HttpStatus.UNAUTHORIZED, "Debes iniciar sesión con un token válido"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> escribirError(response,
                                HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción")))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorRespuesta error = new ErrorRespuesta(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                LocalDateTime.now(clock));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}