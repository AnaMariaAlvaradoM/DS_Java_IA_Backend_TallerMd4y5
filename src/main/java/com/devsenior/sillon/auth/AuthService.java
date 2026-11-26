package com.devsenior.sillon.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

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

    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthService(SalonRepository salonRepository, UsuarioRepository usuarioRepository,
            GeneradorSlug generadorSlug, PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock,
            @Value("${sillon.creditos.registro:10}") int creditosRegistro) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    /** Crea el salón y su dueño en una sola transacción. */
    @Transactional
    public AuthRespuesta registrar(RegistroPeticion peticion) {
        String email = normalizarEmail(peticion.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }

        Salon salon = new Salon();
        salon.setNombre(peticion.salonNombre().trim());
        salon.setSlug(generadorSlug.generarUnico(peticion.salonNombre()));
        salon.setCiudad(peticion.ciudad().trim());
        salon.setDireccion(peticion.direccion().trim());
        salon.setTelefono(peticion.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(clock));
        salon = salonRepository.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(peticion.nombre().trim());
        dueno.setEmail(email);
        dueno.setPassword(passwordEncoder.encode(peticion.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setSalon(salon);
        dueno = usuarioRepository.save(dueno);

        return respuesta(dueno, salon);
    }

    /** Mensaje genérico: no revela si falló el correo o la contraseña. */
    @Transactional(readOnly = true)
    public AuthRespuesta login(LoginPeticion peticion) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(peticion.email()))
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(peticion.password(), u.getPassword()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        return respuesta(usuario, usuario.getSalon());
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthRespuesta respuesta(Usuario usuario, Salon salon) {
        return new AuthRespuesta(jwtService.generar(usuario), "Bearer", jwtService.getExpiraMinutos(),
                usuario.getId(), usuario.getNombre(), usuario.getRol(), salon.getId(), salon.getSlug());
    }
}
