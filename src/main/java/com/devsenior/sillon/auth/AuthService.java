package com.devsenior.sillon.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

/** Registro del salón con su dueño y login (paso 2.3). */
@Service
public class AuthService {

    private static final String MENSAJE_CREDENCIALES = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwt servicioJwt;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthService(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            GeneradorSlug generadorSlug, PasswordEncoder passwordEncoder, ServicioJwt servicioJwt, Clock clock,
            @Value("${sillon.creditos.registro}") int creditosRegistro) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    /** Crea el salón (slug, plan GRATIS, créditos de cortesía, horario por defecto) y su dueño en una sola transacción. */
    @Transactional
    public RespuestaAuth registrar(RegistroRequest peticion) {
        String email = normalizarEmail(peticion.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaDeNegocioException("El correo ya está registrado en la plataforma");
        }

        Salon salon = new Salon();
        salon.setNombre(peticion.salonNombre());
        salon.setSlug(generadorSlug.generar(peticion.salonNombre()));
        salon.setCiudad(peticion.ciudad());
        salon.setDireccion(peticion.direccion());
        salon.setTelefono(peticion.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(clock));
        salonRepository.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(peticion.nombre());
        dueno.setEmail(email);
        dueno.setPassword(passwordEncoder.encode(peticion.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setSalon(salon);
        usuarioRepository.save(dueno);

        return construirRespuesta(dueno, salon);
    }

    /** Responde 401 con un mensaje genérico, sin decir si falló el correo o la contraseña. */
    @Transactional(readOnly = true)
    public RespuestaAuth login(LoginRequest peticion) {
        String email = normalizarEmail(peticion.email());
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES));
        if (!usuario.isActivo() || !passwordEncoder.matches(peticion.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }
        return construirRespuesta(usuario, usuario.getSalon());
    }

    private RespuestaAuth construirRespuesta(Usuario usuario, Salon salon) {
        String token = servicioJwt.generarToken(usuario.getEmail(), usuario.getRol().name(), salon.getId());
        return new RespuestaAuth(
                token,
                "Bearer",
                servicioJwt.getExpiracionMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().name(),
                salon.getId(),
                salon.getSlug());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
