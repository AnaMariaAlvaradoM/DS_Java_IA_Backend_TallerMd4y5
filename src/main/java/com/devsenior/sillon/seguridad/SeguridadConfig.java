package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.ErrorApi;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SeguridadConfig {
    private final ObjectMapper json;
    private final Clock reloj;

    public SeguridadConfig(ObjectMapper json, Clock reloj) {
        this.json = json;
        this.reloj = reloj;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, FiltroJwt filtroJwt) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, exception) ->
                                escribirError(response, HttpStatus.UNAUTHORIZED, "No autenticado"))
                        .accessDeniedHandler((request, response, exception) ->
                                escribirError(response, HttpStatus.FORBIDDEN, "No tiene permiso para esta acción")))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/api/pagos/webhook",
                                "/api/pagos/resultado", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/salon", "/api/salon/**", "/api/estilistas", "/api/estilistas/**",
                                "/api/clientes", "/api/clientes/**", "/api/pagos", "/api/pagos/**",
                                "/api/solicitudes", "/api/solicitudes/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/servicios").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/servicios", "/api/servicios/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado")
                                .hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.POST, "/api/citas/*/recordatorio").hasRole("DUENO")
                        .requestMatchers("/api/citas", "/api/citas/**").hasRole("DUENO")
                        .anyRequest().denyAll())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorApi error = new ErrorApi(estado.value(), estado.getReasonPhrase(), mensaje, LocalDateTime.now(reloj));
        response.getWriter().write(json.writeValueAsString(error));
    }
}
