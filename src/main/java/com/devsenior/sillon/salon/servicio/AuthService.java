package com.devsenior.sillon.salon.servicio;

import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.dto.AuthResponse;
import com.devsenior.sillon.salon.dto.LoginRequest;
import com.devsenior.sillon.salon.dto.RegistroRequest;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.salon.util.SlugUtil;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthService(
            SalonRepository salonRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock,
            @Value("${sillon.negocio.creditos-registro}") int creditosRegistro) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        String slug = generarSlugUnico(request.salonNombre());
        LocalDateTime ahora = LocalDateTime.now(clock);

        Salon salon = new Salon();
        salon.setNombre(request.salonNombre().trim());
        salon.setSlug(slug);
        salon.setCiudad(request.ciudad().trim());
        salon.setDireccion(request.direccion().trim());
        salon.setTelefono(request.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(ahora);
        salon = salonRepository.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(request.nombre().trim());
        dueno.setEmail(emailNormalizado);
        dueno.setPassword(passwordEncoder.encode(request.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setFechaCreacion(ahora);
        dueno.setSalon(salon);
        dueno = usuarioRepository.save(dueno);

        return construirAuthResponse(dueno, salon);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales inválidas"));

        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }

        return construirAuthResponse(usuario, usuario.getSalon());
    }

    private AuthResponse construirAuthResponse(Usuario usuario, Salon salon) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String token = jwtService.generarToken(principal);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiraEnMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol(),
                salon.getId(),
                salon.getSlug());
    }

    private String generarSlugUnico(String nombreSalon) {
        String base = SlugUtil.generarSlug(nombreSalon);
        if (base.isBlank()) {
            base = "salon";
        }
        String candidato = base;
        int sufijo = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
}