package com.devsenior.sillon.seguridad;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<FiltroJwt> registroFiltroJwt(FiltroJwt filtro) {
        FilterRegistrationBean<FiltroJwt> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, FiltroJwt filtro,
            RespuestaErrorSeguridad errores) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, ex) -> errores.escribir(response,
                                HttpStatus.UNAUTHORIZED, "Debe autenticarse con un token válido"))
                        .accessDeniedHandler((request, response, ex) -> errores.escribir(response,
                                HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción")))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/api/pagos/webhook",
                                "/api/pagos/resultado", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/citas").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole("DUENO", "ESTILISTA")
                        .requestMatchers("/api/salon", "/api/salon/**", "/api/servicios", "/api/servicios/**",
                                "/api/estilistas", "/api/estilistas/**", "/api/clientes", "/api/citas", "/api/citas/**",
                                "/api/pagos/**", "/api/solicitudes", "/api/solicitudes/**").hasRole("DUENO")
                        .anyRequest().authenticated())
                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
