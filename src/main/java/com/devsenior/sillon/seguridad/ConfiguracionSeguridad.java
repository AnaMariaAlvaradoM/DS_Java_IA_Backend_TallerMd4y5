package com.devsenior.sillon.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(ServicioUsuariosSeguridad usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, FiltroJwt filtro,
            RespuestaErrorSeguridad errores) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint((solicitud, respuesta, error) ->
                                errores.escribir(respuesta, HttpStatus.UNAUTHORIZED, "Autenticación requerida"))
                        .accessDeniedHandler((solicitud, respuesta, error) ->
                                errores.escribir(respuesta, HttpStatus.FORBIDDEN, "Acceso no permitido")))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/pagos/resultado").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/salon", "/api/salon/**", "/api/estilistas/**", "/api/clientes/**",
                                "/api/solicitudes/**", "/api/pagos/**").hasRole("DUENO")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/servicios", "/api/servicios/")
                                .hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/servicios", "/api/servicios/**").hasRole("DUENO")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/citas/**").hasRole("DUENO")
                        .anyRequest().denyAll())
                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
