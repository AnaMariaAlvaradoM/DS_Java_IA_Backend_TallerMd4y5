package com.devsenior.sillon.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.NegocioConfig;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.JwtServicio;
import com.devsenior.sillon.usuario.NormalizadorCorreo;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class AuthServicio {

    static final String MENSAJE_LOGIN_FALLIDO = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final JwtServicio jwtServicio;
    private final NegocioConfig negocio;
    private final Clock reloj;

    /** Hash de una contrasena cualquiera, solo para gastar el mismo tiempo cuando el correo no existe. */
    private final String hashFalso;

    public AuthServicio(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            GeneradorSlug generadorSlug, PasswordEncoder passwordEncoder, JwtServicio jwtServicio,
            NegocioConfig negocio, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.jwtServicio = jwtServicio;
        this.negocio = negocio;
        this.reloj = reloj;
        this.hashFalso = passwordEncoder.encode("contrasena-falsa-para-igualar-tiempos");
    }

    /** Crea el salon y su dueno en una sola transaccion: si algo falla, no queda ni uno ni otro. */
    @Transactional
    public AuthRespuesta registrar(RegistroPeticion peticion) {
        String email = NormalizadorCorreo.normalizar(peticion.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo.");
        }

        String nombreSalon = peticion.salonNombre().trim();
        Salon salon = new Salon();
        salon.setNombre(nombreSalon);
        salon.setSlug(generadorSlug.generar(nombreSalon));
        salon.setCiudad(peticion.ciudad().trim());
        salon.setDireccion(limpiarOpcional(peticion.direccion()));
        salon.setTelefono(limpiarOpcional(peticion.telefono()));
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(negocio.creditosRegistro());
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
        salonRepository.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(peticion.nombre().trim());
        dueno.setEmail(email);
        dueno.setPassword(passwordEncoder.encode(peticion.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setSalon(salon);
        usuarioRepository.save(dueno);

        return aRespuesta(dueno);
    }

    /**
     * Mismo mensaje y mismo codigo (401) si falla el correo, la contrasena o la cuenta esta desactivada:
     * quien escribe no debe poder averiguar que correos existen.
     */
    @Transactional(readOnly = true)
    public AuthRespuesta iniciarSesion(LoginPeticion peticion) {
        String email = NormalizadorCorreo.normalizar(peticion.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        // Si el correo no existe se compara igual contra un hash falso (BCrypt es lento a proposito):
        // sin esto, responder rapido delataria que el correo no esta registrado.
        String hash = usuario != null ? usuario.getPassword() : hashFalso;
        boolean contrasenaCorrecta = passwordEncoder.matches(peticion.password(), hash);

        if (usuario == null || !contrasenaCorrecta || !usuario.isActivo()) {
            throw new CredencialesInvalidasException(MENSAJE_LOGIN_FALLIDO);
        }
        return aRespuesta(usuario);
    }

    private AuthRespuesta aRespuesta(Usuario usuario) {
        return new AuthRespuesta(
                jwtServicio.generar(usuario),
                "Bearer",
                jwtServicio.getExpiracionMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getSalon().getId(),
                usuario.getSalon().getSlug());
    }

    private String limpiarOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
