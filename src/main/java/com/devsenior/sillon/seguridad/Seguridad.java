package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.compartido.Fallos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Sillón API", version = "1.0"))
@SecurityScheme(
        name = "sesion",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class Seguridad {
    @Bean
    BCryptPasswordEncoder passwords() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain cadenaDeFiltros(
            HttpSecurity http, Tokens tokens, Datos datos, ObjectMapper json, Clock reloj)
            throws Exception {
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new FiltroJwt(tokens, datos), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e ->
                        e.authenticationEntryPoint((r, res, ex) ->
                                        error(
                                                res,
                                                json,
                                                reloj,
                                                401,
                                                "Inicia sesión con un token válido"))
                                .accessDeniedHandler((r, res, ex) ->
                                        error(
                                                res,
                                                json,
                                                reloj,
                                                403,
                                                "Tu rol no permite esta acción")))
                .authorizeHttpRequests(a ->
                        a.requestMatchers(
                                        "/api/auth/**",
                                        "/api/publico/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/error")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/pagos/webhook")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/pagos/resultado")
                                .permitAll()
                                .requestMatchers("/api/admin/**")
                                .hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/citas")
                                .hasAnyRole("DUENO", "ESTILISTA")
                                .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado")
                                .hasAnyRole("DUENO", "ESTILISTA")
                                .requestMatchers("/api/**")
                                .hasRole("DUENO")
                                .anyRequest()
                                .denyAll())
                .build();
    }

    private void error(
            HttpServletResponse res, ObjectMapper json, Clock clock, int status, String mensaje)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json.writeValueAsString(Fallos.error(clock, status, mensaje)));
    }
}
