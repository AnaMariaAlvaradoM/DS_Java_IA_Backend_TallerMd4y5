package com.devsenior.sillon.auth;

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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final SalonRepository salones;
    private final UsuarioRepository usuarios;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock reloj;
    private final int creditosRegistro;

    public AuthService(SalonRepository salones, UsuarioRepository usuarios, GeneradorSlug generadorSlug,
                       PasswordEncoder encoder, JwtService jwt, Clock reloj,
                       @Value("${sillon.creditos.registro}") int creditosRegistro) {
        this.salones = salones;
        this.usuarios = usuarios;
        this.generadorSlug = generadorSlug;
        this.encoder = encoder;
        this.jwt = jwt;
        this.reloj = reloj;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarios.existsByEmail(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Salon salon = new Salon();
        salon.setNombre(request.salonNombre().trim());
        salon.setSlug(generadorSlug.generar(request.salonNombre()));
        salon.setCiudad(request.ciudad().trim());
        salon.setDireccion(request.direccion().trim());
        salon.setTelefono(request.telefono().trim());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(reloj));

        Usuario dueno = new Usuario();
        dueno.setNombre(request.nombre().trim());
        dueno.setEmail(email);
        dueno.setPassword(encoder.encode(request.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        try {
            dueno.setSalon(salones.saveAndFlush(salon));
            usuarios.saveAndFlush(dueno);
        } catch (DataIntegrityViolationException exception) {
            throw new ReglaNegocioException("El correo o el nombre público del salón ya está registrado");
        }
        return responder(dueno);
    }

    @Transactional(readOnly = true)
    public AuthResponse iniciarSesion(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(normalizarEmail(request.email()))
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales inválidas"));
        if (!usuario.isActivo() || !encoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }
        return responder(usuario);
    }

    private AuthResponse responder(Usuario usuario) {
        return new AuthResponse(jwt.generarToken(usuario), "Bearer", jwt.getExpiraEnMinutos(),
                usuario.getId(), usuario.getNombre(), usuario.getRol(),
                usuario.getSalon().getId(), usuario.getSalon().getSlug());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
