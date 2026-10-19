package com.devsenior.sillon.auth;

import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final SalonRepository salones;
    private final UsuarioRepository usuarios;
    private final GeneradorSlug slugs;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final int creditosRegistro;

    public AuthService(SalonRepository salones, UsuarioRepository usuarios, GeneradorSlug slugs,
            PasswordEncoder passwords, JwtService jwt, @Value("${sillon.creditos-registro}") int creditosRegistro) {
        this.salones = salones;
        this.usuarios = usuarios;
        this.slugs = slugs;
        this.passwords = passwords;
        this.jwt = jwt;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarios.existsByEmail(request.email())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Salon salon = new Salon();
        salon.setNombre(request.salonNombre());
        salon.setSlug(slugs.generar(request.salonNombre()));
        salon.setCiudad(request.ciudad());
        salon.setDireccion(request.direccion());
        salon.setTelefono(request.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salones.save(salon);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwords.encode(request.password()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        usuarios.saveAndFlush(usuario);
        return respuesta(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(request.email())
                .orElseThrow(AuthService::credencialesInvalidas);
        if (!usuario.isActivo() || !passwords.matches(request.password(), usuario.getPassword())) {
            throw credencialesInvalidas();
        }
        return respuesta(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        return new AuthResponse(jwt.generar(usuario), "Bearer", jwt.getExpiraEnMinutos(), usuario.getId(),
                usuario.getNombre(), usuario.getRol(), usuario.getSalon().getId(), usuario.getSalon().getSlug());
    }

    private static CredencialesInvalidasException credencialesInvalidas() {
        return new CredencialesInvalidasException("Correo o contraseña incorrectos");
    }
}
