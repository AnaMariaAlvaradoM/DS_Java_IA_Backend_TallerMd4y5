package com.devsenior.sillon.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(
            HttpSecurity http,
            ServicioJwt servicioJwt,
            ServicioDetallesUsuario servicioDetallesUsuario,
            ManejadorSeguridad manejadorSeguridad)
            throws Exception {

        FiltroJwt filtroJwt = new FiltroJwt(
                servicioJwt,
                servicioDetallesUsuario);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(manejadorSeguridad)
                        .accessDeniedHandler(manejadorSeguridad))

                .authorizeHttpRequests(permisos -> permisos

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/publico/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/pagos/webhook")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pagos/resultado")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/admin/salones")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/servicios",
                                "/api/citas")
                        .hasAnyRole("DUENO", "ESTILISTA")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/citas/*/estado")
                        .hasAnyRole("DUENO", "ESTILISTA")

                        .requestMatchers(
                                "/api/salon",
                                "/api/salon/**",
                                "/api/servicios",
                                "/api/servicios/**",
                                "/api/estilistas",
                                "/api/clientes",
                                "/api/citas",
                                "/api/citas/**",
                                "/api/pagos/**",
                                "/api/solicitudes",
                                "/api/solicitudes/**")
                        .hasRole("DUENO")

                        .anyRequest()
                        .denyAll())

                .addFilterBefore(
                        filtroJwt,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}