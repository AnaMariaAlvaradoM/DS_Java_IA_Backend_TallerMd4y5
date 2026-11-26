package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.devsenior.sillon.usuario.UsuarioRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SeguridadConfig {

    private static final String DUENO = "DUENO";
    private static final String ESTILISTA = "ESTILISTA";
    private static final String ADMIN = "ADMIN";

    private final Clock clock;

    public SeguridadConfig(Clock clock) {
        this.clock = clock;
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, JwtService jwtService,
            UsuarioRepository usuarioRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribirError(res, HttpStatus.UNAUTHORIZED,
                                "Necesitas iniciar sesión con un token válido"))
                        .accessDeniedHandler((req, res, ex) -> escribirError(res, HttpStatus.FORBIDDEN,
                                "Tu rol no tiene permiso para esta acción")))
                .authorizeHttpRequests(auth -> auth
                        // Públicas
                        .requestMatchers("/api/auth/**", "/api/publico/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resultado").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Mi salón
                        .requestMatchers("/api/salon/**", "/api/salon").hasRole(DUENO)
                        // Servicios: el estilista solo lista
                        .requestMatchers(HttpMethod.GET, "/api/servicios").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers("/api/servicios/**", "/api/servicios").hasRole(DUENO)
                        // Equipo y clientes
                        .requestMatchers("/api/estilistas/**", "/api/estilistas").hasRole(DUENO)
                        .requestMatchers("/api/clientes/**", "/api/clientes").hasRole(DUENO)
                        // Agenda
                        .requestMatchers(HttpMethod.GET, "/api/citas").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/estado").hasAnyRole(DUENO, ESTILISTA)
                        .requestMatchers("/api/citas/**", "/api/citas").hasRole(DUENO)
                        // Solicitudes, pagos y administración
                        .requestMatchers("/api/solicitudes/**", "/api/solicitudes").hasRole(DUENO)
                        .requestMatchers("/api/pagos/**").hasRole(DUENO)
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .addFilterBefore(new FiltroJwt(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Evita que Spring cree un usuario en memoria con contraseña generada. */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return email -> usuarioRepository.findByEmail(email)
                .map(UsuarioPrincipal::de)
                .orElseThrow(() -> new UsernameNotFoundException("No existe"));
    }

    /** 401 y 403 con el mismo formato único de error del resto de la API. */
    private void escribirError(HttpServletResponse res, HttpStatus estado, String mensaje) throws IOException {
        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        String json = "{\"codigo\":" + estado.value()
                + ",\"error\":\"" + estado.getReasonPhrase()
                + "\",\"mensaje\":\"" + mensaje
                + "\",\"fecha\":\"" + LocalDateTime.now(clock) + "\"}";
        res.getWriter().write(json);
    }
}
