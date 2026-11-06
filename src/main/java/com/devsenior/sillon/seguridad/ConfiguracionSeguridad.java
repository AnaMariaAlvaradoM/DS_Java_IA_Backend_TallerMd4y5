
package com.devsenior.sillon.seguridad;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    public SecurityFilterChain filtroSeguridad(
        HttpSecurity http,
        FiltroJwt filtroJwt,
        PuntoEntradaAutenticacion puntoEntradaAutenticacion,
        ManejadorAccesoDenegado manejadorAccesoDenegado) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sesion -> sesion
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(excepciones -> excepciones
            .authenticationEntryPoint(puntoEntradaAutenticacion)
            .accessDeniedHandler(manejadorAccesoDenegado))
            .authorizeHttpRequests(reglas -> reglas

                // Rutas públicas
                .requestMatchers(
                        "/api/auth/**",
                        "/api/publico/**",
                        "/api/pagos/webhook",
                        "/api/pagos/resultado",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error"
                ).permitAll()

                // Administración
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")

                // Gestión del salón
                .requestMatchers("/api/salon", "/api/salon/**")
                .hasRole("DUENO")

                // Gestión exclusiva del dueño
                .requestMatchers(
                        "/api/estilistas/**",
                        "/api/clientes/**",
                        "/api/solicitudes/**",
                        "/api/pagos/**"
                ).hasRole("DUENO")

                // Servicios: consulta
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/servicios",
                        "/api/servicios/**"
                ).hasAnyRole("DUENO", "ESTILISTA")

                // Servicios: creación y edición
                .requestMatchers(
                        "/api/servicios",
                        "/api/servicios/**"
                ).hasRole("DUENO")

                // Recordatorios de citas
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/citas/*/recordatorio"
                ).hasRole("DUENO")

                // Crear citas
                .requestMatchers(HttpMethod.POST, "/api/citas")
                .hasRole("DUENO")

                // Consultar agenda
                .requestMatchers(HttpMethod.GET, "/api/citas")
                .hasAnyRole("DUENO", "ESTILISTA")

                // Cambiar estados de citas
                .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/citas/*/estado"
                ).hasAnyRole("DUENO", "ESTILISTA")

                // Denegar cualquier otra operación
                .anyRequest().denyAll()
            )

            // Incorporar nuestro filtro JWT
            .addFilterBefore(
                    filtroJwt,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<FiltroJwt> registroFiltroJwt(
            FiltroJwt filtroJwt) {

        FilterRegistrationBean<FiltroJwt> registro =
                new FilterRegistrationBean<>(filtroJwt);

        registro.setEnabled(false);
        return registro;
    }
}
