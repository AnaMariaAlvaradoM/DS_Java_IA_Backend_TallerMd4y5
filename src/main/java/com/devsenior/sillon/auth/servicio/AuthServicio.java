package com.devsenior.sillon.auth.servicio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.auth.dto.SolicitudLogin;
import com.devsenior.sillon.auth.dto.SolicitudRegistro;
import com.devsenior.sillon.auth.mapper.AuthMapper;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoAutenticado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.salon.servicio.GeneradorSlug;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class AuthServicio {

    private final SalonRepositorio salonRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final GeneradorSlug generadorSlug;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwt servicioJwt;
    private final AuthMapper authMapper;
    private final Clock reloj;

    public AuthServicio(
            SalonRepositorio salonRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            GeneradorSlug generadorSlug,
            PasswordEncoder passwordEncoder,
            ServicioJwt servicioJwt,
            AuthMapper authMapper,
            Clock reloj) {

        this.salonRepositorio = salonRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.generadorSlug = generadorSlug;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
        this.authMapper = authMapper;
        this.reloj = reloj;
    }

    @Transactional
    public RespuestaAuth registrar(SolicitudRegistro solicitud) {

        String email = normalizarEmail(solicitud.email());

        if (usuarioRepositorio.existsByEmail(email)) {
            throw new ExcepcionConflicto(
                    "El correo electrónico ya está registrado.");
        }

        if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ExcepcionSolicitudInvalida(
                    "password: excede el límite de 72 bytes de BCrypt.");
        }

        Salon salon = new Salon();

        salon.setNombre(solicitud.salonNombre());
        salon.setSlug(generadorSlug.generar(solicitud.salonNombre()));
        salon.setCiudad(solicitud.ciudad());
        salon.setDireccion(solicitud.direccion());
        salon.setTelefono(solicitud.telefono());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(ConfiguracionSalon.predeterminada());
        salon.setFechaCreacion(LocalDateTime.now(reloj));

        salon = salonRepositorio.save(salon);

        Usuario usuario = new Usuario();

        usuario.setNombre(solicitud.nombre());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(solicitud.password()));
        usuario.setRol(Rol.DUENO);
        usuario.setActivo(true);
        usuario.setSalon(salon);

        usuario = usuarioRepositorio.save(usuario);

        return generarRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public RespuestaAuth login(SolicitudLogin solicitud) {

        if (solicitud.email() == null
                || solicitud.email().isBlank()
                || solicitud.password() == null
                || solicitud.password().isBlank()
                || solicitud.password().getBytes(StandardCharsets.UTF_8).length > 72) {

            throw credencialesInvalidas();
        }

        String email = normalizarEmail(solicitud.email());

        Usuario usuario = usuarioRepositorio.findByEmail(email)
                .orElseThrow(this::credencialesInvalidas);

        if (!usuario.isActivo()
                || !passwordEncoder.matches(
                        solicitud.password(), usuario.getPassword())) {

            throw credencialesInvalidas();
        }

        return generarRespuesta(usuario);
    }

    private RespuestaAuth generarRespuesta(Usuario usuario) {

        String token = servicioJwt.generarToken(
                new UsuarioPrincipal(usuario));

        return authMapper.aRespuesta(
                usuario,
                token,
                servicioJwt.getExpiraEnMinutos());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ExcepcionNoAutenticado credencialesInvalidas() {
        return new ExcepcionNoAutenticado("Credenciales inválidas.");
    }

}