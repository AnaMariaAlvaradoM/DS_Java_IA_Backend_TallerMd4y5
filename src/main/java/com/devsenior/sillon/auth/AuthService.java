package com.devsenior.sillon.auth;

import com.devsenior.sillon.auth.dto.AuthResponseDto;
import com.devsenior.sillon.auth.dto.LoginRequestDto;
import com.devsenior.sillon.auth.dto.RegistroRequestDto;
import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.CredencialesInvalidasExcepcion;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.salon.SlugService;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SalonRepository salonRepository;
    private final UsuarioRepository usuarioRepository;
    private final SlugService slugService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            SalonRepository salonRepository,
            UsuarioRepository usuarioRepository,
            SlugService slugService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.salonRepository = salonRepository;
        this.usuarioRepository = usuarioRepository;
        this.slugService = slugService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDto registrar(RegistroRequestDto request) {
        String emailLimpio = request.email().trim().toLowerCase();

        // Verificación de correo único en toda la plataforma (409 Conflict)
        if (usuarioRepository.existsByEmail(emailLimpio)) {
            throw new ConflictoExcepcion("El correo electronico ya esta registrado");
        }

        // Crear el salón en plan GRATIS con 10 créditos y horario por defecto
        Salon salon = new Salon();
        salon.setNombre(request.salonNombre().trim());
        salon.setSlug(slugService.generarSlugUnico(request.salonNombre()));
        salon.setCiudad(request.ciudad().trim());
        salon.setDireccion(request.direccion().trim());
        salon.setTelefono(request.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(ConfiguracionSalon.crearPorDefecto());
        salon.setFechaCreacion(LocalDateTime.now());

        salon = salonRepository.save(salon);

        // Crear el usuario dueño asociado al salón
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(emailLimpio);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);

        usuario = usuarioRepository.save(usuario);

        UsuarioPrincipal usuarioPrincipal = UsuarioPrincipal.desdeEntidad(usuario);
        String token = jwtService.generarToken(usuarioPrincipal);

        return new AuthResponseDto(
                token,
                "Bearer",
                jwtService.getExpiracionMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().name(),
                salon.getId(),
                salon.getSlug()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String emailLimpio = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailLimpio)
                .orElseThrow(() -> new com.devsenior.sillon.compartido.CredencialesInvalidasExcepcion("Credenciales incorrectas"));

        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasExcepcion("Credenciales incorrectas");
        }

        UsuarioPrincipal usuarioPrincipal = UsuarioPrincipal.desdeEntidad(usuario);
        String token = jwtService.generarToken(usuarioPrincipal);

        Long salonId = usuario.getSalon() != null ? usuario.getSalon().getId() : null;
        String salonSlug = usuario.getSalon() != null ? usuario.getSalon().getSlug() : null;

        return new AuthResponseDto(
                token,
                "Bearer",
                jwtService.getExpiracionMinutos(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().name(),
                salonId,
                salonSlug
        );
    }
}
