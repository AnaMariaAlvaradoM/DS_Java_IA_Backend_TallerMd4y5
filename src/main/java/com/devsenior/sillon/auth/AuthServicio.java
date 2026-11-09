package com.devsenior.sillon.auth;

import com.devsenior.sillon.compartido.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.GeneradorSlug;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServicio {
    private final SalonRepositorio salones;
    private final UsuarioRepositorio usuarios;
    private final GeneradorSlug slugs;
    private final PasswordEncoder encoder;
    private final AuthenticationManager autenticacion;
    private final ServicioJwt jwt;
    private final Clock clock;
    private final int creditosRegistro;

    public AuthServicio(SalonRepositorio salones, UsuarioRepositorio usuarios, GeneradorSlug slugs,
            PasswordEncoder encoder, AuthenticationManager autenticacion, ServicioJwt jwt, Clock clock,
            @Value("${sillon.creditos.registro}") int creditosRegistro) {
        this.salones = salones;
        this.usuarios = usuarios;
        this.slugs = slugs;
        this.encoder = encoder;
        this.autenticacion = autenticacion;
        this.jwt = jwt;
        this.clock = clock;
        this.creditosRegistro = creditosRegistro;
    }

    @Transactional
    public AuthRespuesta registrar(RegistroSolicitud solicitud) {
        String email = normalizarEmail(solicitud.email());
        if (usuarios.existsByEmail(email)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Salon salon = new Salon();
        salon.setNombre(solicitud.salonNombre());
        salon.setSlug(slugs.generar(solicitud.salonNombre()));
        salon.setCiudad(solicitud.ciudad());
        salon.setDireccion(solicitud.direccion());
        salon.setTelefono(solicitud.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditosRegistro);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.now(clock));
        salones.save(salon);

        Usuario dueno = new Usuario();
        dueno.setNombre(solicitud.nombre());
        dueno.setEmail(email);
        dueno.setPassword(encoder.encode(solicitud.password()));
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        dueno.setSalon(salon);
        try {
            usuarios.saveAndFlush(dueno);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ReglaNegocioException("El correo o el nombre del salón ya está registrado");
        }
        return respuesta(dueno);
    }

    @Transactional(readOnly = true)
    public AuthRespuesta login(LoginSolicitud solicitud) {
        String email = normalizarEmail(solicitud.email());
        try {
            autenticacion.authenticate(new UsernamePasswordAuthenticationToken(email, solicitud.password()));
        } catch (AuthenticationException excepcion) {
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales inválidas"));
        return respuesta(usuario);
    }

    private AuthRespuesta respuesta(Usuario usuario) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return new AuthRespuesta(jwt.generar(principal), "Bearer", jwt.getExpiraEnMinutos(),
                usuario.getId(), usuario.getNombre(), usuario.getRol(),
                principal.getSalonId(), usuario.getSalon().getSlug());
    }

    private String normalizarEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
