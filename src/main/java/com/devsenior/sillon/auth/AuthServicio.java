package com.devsenior.sillon.auth;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.salon.SalonServicio;
import com.devsenior.sillon.seguridad.JwtServicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class AuthServicio {

    private static final String MENSAJE_CREDENCIALES = "Credenciales inválidas";
    private static final String MENSAJE_CORREO_EXISTENTE = "El correo ya está registrado";
    private static final int CREDITOS_INICIALES = 10;

    private final UsuarioRepository usuarioRepository;
    private final SalonRepository salonRepository;
    private final SalonServicio salonServicio;
    private final PasswordEncoder passwordEncoder;
    private final JwtServicio jwtServicio;
    private final Clock reloj;

    public AuthServicio(UsuarioRepository usuarioRepository, SalonRepository salonRepository,
            SalonServicio salonServicio, PasswordEncoder passwordEncoder, JwtServicio jwtServicio,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.salonRepository = salonRepository;
        this.salonServicio = salonServicio;
        this.passwordEncoder = passwordEncoder;
        this.jwtServicio = jwtServicio;
        this.reloj = reloj;
    }

    @Transactional
    public AuthRespuesta registrar(RegistroSolicitud solicitud) {
        String email = solicitud.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(MENSAJE_CORREO_EXISTENTE);
        }

        Salon salon = new Salon();
        salon.setNombre(solicitud.salonNombre().trim());
        salon.setSlug(salonServicio.generarSlug(solicitud.salonNombre().trim()));
        salon.setCiudad(solicitud.ciudad().trim());
        salon.setDireccion(solicitud.direccion().trim());
        salon.setTelefono(solicitud.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(CREDITOS_INICIALES);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(reloj));
        salon = salonRepository.save(salon);

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(solicitud.password()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        usuario = usuarioRepository.save(usuario);

        return armarRespuesta(usuario, salon);
    }

    @Transactional
    public AuthRespuesta iniciarSesion(LoginSolicitud solicitud) {
        String email = solicitud.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES));
        boolean coincide = passwordEncoder.matches(solicitud.password(), usuario.getPassword());
        if (!coincide || !usuario.isActivo()) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }
        return armarRespuesta(usuario, usuario.getSalon());
    }

    private AuthRespuesta armarRespuesta(Usuario usuario, Salon salon) {
        String token = jwtServicio.generar(usuario.getEmail(), usuario.getRol(), salon.getId());
        return new AuthRespuesta(
                token,
                "Bearer",
                JwtServicio.MINUTOS_EXPIRACION,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                salon.getId(),
                salon.getSlug());
    }
}
