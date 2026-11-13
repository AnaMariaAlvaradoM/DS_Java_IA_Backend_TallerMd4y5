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

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final FiltroJwt filtroJwt;
    private final PuntoEntradaAutenticacion puntoDeEntrada;
    private final ManejadorAccesoDenegado accesoDenegado;

    public SeguridadConfig(FiltroJwt filtroJwt, PuntoEntradaAutenticacion puntoDeEntrada,
            ManejadorAccesoDenegado accesoDenegado) {
        this.filtroJwt = filtroJwt;
        this.puntoDeEntrada = puntoDeEntrada;
        this.accesoDenegado = accesoDenegado;
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/publico/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resultado").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/salon/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.POST, "/api/servicios").hasRole("DUENO")
                        .requestMatchers(HttpMethod.PUT, "/api/servicios/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/servicios")
                                .hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/estilistas/**", "/api/clientes/**",
                                "/api/solicitudes/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.POST, "/api/citas",
                                "/api/citas/*/recordatorio").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/**")
                                .hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.POST, "/api/pagos/suscripcion",
                                "/api/pagos/creditos").hasRole("DUENO")
                        .anyRequest().authenticated())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(puntoDeEntrada)
                        .accessDeniedHandler(accesoDenegado))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
