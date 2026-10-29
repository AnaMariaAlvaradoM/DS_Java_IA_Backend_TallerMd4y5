package com.devsenior.sillon.auth;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthService(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            GeneradorSlug generadorSlug, PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock,
            @Value("${sillon.creditos.registro}") int creditosRegistro) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AutenticacionRespuesta registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }

        Salon salon = new Salon();
        salon.setNombre(request.salonNombre());
        salon.setSlug(generadorSlug.generar(request.salonNombre()));
        salon.setCiudad(request.ciudad());
        salon.setDireccion(request.direccion());
        salon.setTelefono(request.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(clock));
        Salon salonGuardado = salonRepository.save(salon);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salonGuardado);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return construirRespuesta(usuarioGuardado, salonGuardado);
    }

    public AutenticacionRespuesta login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contrasena invalidos"));
        return construirRespuesta(usuario, usuario.getSalon());
    }

    private AutenticacionRespuesta construirRespuesta(Usuario usuario, Salon salon) {
        String token = jwtService.generarToken(usuario);
        return new AutenticacionRespuesta(token, "Bearer", jwtService.getExpiraEnMinutos(), usuario.getId(),
                usuario.getNombre(), usuario.getRol().name(), salon.getId(), salon.getSlug());
    }

    private String normalizarEmail(String email) {
        return email.strip().toLowerCase().replaceAll("\\s+", "");
    }
}
