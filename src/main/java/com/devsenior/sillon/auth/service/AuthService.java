package com.devsenior.sillon.auth.service;

import com.devsenior.sillon.auth.dto.AuthResponse;
import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.salon.service.GeneradorSlugService;
import com.devsenior.sillon.security.JwtService;
import com.devsenior.sillon.security.UsuarioPrincipal;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Transactional
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final GeneradorSlugService generadorSlugService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LocalDate fechaCreacion;

    public AuthService(UsuarioRepository usuarioRepository, SalonRepository salonRepository, GeneradorSlugService generadorSlugService, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, LocalDate fechaCreacion) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.generadorSlugService = generadorSlugService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.fechaCreacion = fechaCreacion;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String emailNormalizado = request.email() == null ? null : request.email().trim();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado en la plataforma");
        }

        String nombreSalon = request.nombreSalon() == null ? "" : request.nombreSalon().trim();
        String slug = generadorSlugService.generarSlugUnico(nombreSalon);

        Salon salon = new Salon();
        salon.setSlug(slug);
        salon.setCiudad("Bogotá");
        salon.setDireccion(request.direccion());
        salon.setTelefono(request.telefono() == null ? "" : request.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setStripeSubscriptionId("SIN_SUSCRIPCION");
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setNombresalon(nombreSalon);
        salon.setFechaCreacion(fechaCreacion);

        Salon salonGuardado = salonRepository.save(salon);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(emailNormalizado);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salonGuardado);


        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        UsuarioPrincipal principal = UsuarioPrincipal.build(usuario);
        String token = jwtService.generarToken(principal);

        return AuthResponse.of(
                token,
                usuario.getEmail(),
                usuario.getRol().name(),
                salonGuardado.getId(),
                salonGuardado.getSlug()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new NoEncontradoException("No existe un usuario con ese email"));
        String token = jwtService.generarToken(UsuarioPrincipal.build(usuario));
        return  AuthResponse.of(token, usuario.getEmail(), usuario.getRol().name(), usuario.getSalon().getId(), usuario.getSalon().getSlug());
    }
}