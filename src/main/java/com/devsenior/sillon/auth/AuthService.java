package com.devsenior.sillon.auth;

import com.devsenior.sillon.auth.dto.AuthRespuesta;
import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.salon.SlugService;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final SlugService slugService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthService(
            SalonRepository salonRepository,
            UsuarioRepository usuarioRepository,
            SlugService slugService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock,
            @Value("${sillon.creditos.registro:10}") int creditosRegistro
    ) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.slugService = slugService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AuthRespuesta registrar(RegistroRequest request) {
        String emailNormalizado = request.email().toLowerCase().trim();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado en la plataforma");
        }

        String slug = slugService.generarSlug(request.salonNombre());

        Salon salon = new Salon();
        salon.setNombre(request.salonNombre().trim());
        salon.setSlug(slug);
        salon.setCiudad(request.ciudad().trim());
        salon.setDireccion(request.direccion().trim());
        salon.setTelefono(request.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(clock));

        salon = salonRepository.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(request.nombre().trim());
        dueno.setEmail(emailNormalizado);
        dueno.setPassword(passwordEncoder.encode(request.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setSalon(salon);

        dueno = usuarioRepository.save(dueno);

        String token = jwtService.generarToken(dueno.getEmail(), dueno.getRol(), salon.getId());

        return new AuthRespuesta(
                token,
                "Bearer",
                jwtService.getExpiracionMinutos(),
                dueno.getId(),
                dueno.getNombre(),
                dueno.getRol(),
                salon.getId(),
                salon.getSlug()
        );
    }

    @Transactional(readOnly = true)
    public AuthRespuesta login(LoginRequest request) {
        String emailNormalizado = request.email().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        Salon salon = usuario.getSalon();
        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol(), salon.getId());

        return new AuthRespuesta(
                token,
                "Bearer",
                jwtService.getExpiracionMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                salon.getId(),
                salon.getSlug()
        );
    }
}
