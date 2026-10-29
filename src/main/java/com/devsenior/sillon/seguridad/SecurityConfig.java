package com.devsenior.sillon.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
            UserDetailsService userDetailsService, JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService,
                userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resultado").permitAll()

                        .requestMatchers("/api/salon/**").hasRole("DUENO")

                        .requestMatchers(HttpMethod.POST, "/api/servicios").hasRole("DUENO")
                        .requestMatchers(HttpMethod.PUT, "/api/servicios/**").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/servicios").hasAnyRole("DUENO", "ESTILISTA")

                        .requestMatchers("/api/estilistas/**").hasRole("DUENO")
                        .requestMatchers("/api/clientes/**").hasRole("DUENO")

                        .requestMatchers(HttpMethod.POST, "/api/citas").hasRole("DUENO")
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.POST, "/api/citas/*/recordatorio").hasRole("DUENO")

                        .requestMatchers("/api/solicitudes/**").hasRole("DUENO")

                        .requestMatchers(HttpMethod.POST, "/api/pagos/suscripcion", "/api/pagos/creditos")
                        .hasRole("DUENO")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
