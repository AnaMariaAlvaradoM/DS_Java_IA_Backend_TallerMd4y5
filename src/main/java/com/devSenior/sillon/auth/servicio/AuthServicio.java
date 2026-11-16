package com.devSenior.sillon.auth.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.auth.dto.RespuestaAuth;
import com.devSenior.sillon.auth.dto.SolicitudLogin;
import com.devSenior.sillon.auth.dto.SolicitudRegistro;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoAutenticadoException;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.salon.servicio.GeneradorSlug;
import com.devSenior.sillon.seguridad.JwtServicio;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class AuthServicio {

	static final String MENSAJE_LOGIN = "Correo o contraseña incorrectos";

	private final UsuarioRepositorio usuarioRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final GeneradorSlug generadorSlug;
	private final PasswordEncoder passwordEncoder;
	private final JwtServicio jwtServicio;
	private final Clock reloj;
	private final int creditosRegistro;

	public AuthServicio(UsuarioRepositorio usuarioRepositorio, SalonRepositorio salonRepositorio,
			GeneradorSlug generadorSlug, PasswordEncoder passwordEncoder, JwtServicio jwtServicio, Clock reloj,
			@Value("${sillon.creditos.registro}") int creditosRegistro) {
		this.usuarioRepositorio = usuarioRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.generadorSlug = generadorSlug;
		this.passwordEncoder = passwordEncoder;
		this.jwtServicio = jwtServicio;
		this.reloj = reloj;
		this.creditosRegistro = creditosRegistro;
	}

	@Transactional
	public RespuestaAuth registrar(SolicitudRegistro solicitud) {
		String email = normalizarCorreo(solicitud.email());
		if (usuarioRepositorio.existsByEmail(email)) {
			throw new ConflictoException("El correo ya está registrado");
		}
		Salon salon = new Salon();
		salon.setNombre(solicitud.salonNombre().trim());
		salon.setSlug(generadorSlug.generar(solicitud.salonNombre()));
		salon.setCiudad(solicitud.ciudad().trim());
		salon.setDireccion(solicitud.direccion().trim());
		salon.setTelefono(solicitud.telefono().trim());
		salon.setPlan(Plan.GRATIS);
		salon.setCreditosIa(creditosRegistro);
		salon.setConfiguracion(ConfiguracionSalon.porDefecto());
		salon.setFechaCreacion(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
		salon = salonRepositorio.save(salon);

		Usuario dueno = new Usuario();
		dueno.setNombre(solicitud.nombre().trim());
		dueno.setEmail(email);
		dueno.setPassword(passwordEncoder.encode(solicitud.password()));
		dueno.setRol(Rol.DUENO);
		dueno.setActivo(true);
		dueno.setSalon(salon);
		dueno = usuarioRepositorio.save(dueno);
		return responder(dueno);
	}

	public RespuestaAuth login(SolicitudLogin solicitud) {
		String email = normalizarCorreo(solicitud.email());
		Usuario usuario = usuarioRepositorio.findByEmail(email)
				.orElseThrow(() -> new NoAutenticadoException(MENSAJE_LOGIN));
		if (!usuario.isActivo() || !passwordEncoder.matches(solicitud.password(), usuario.getPassword())) {
			throw new NoAutenticadoException(MENSAJE_LOGIN);
		}
		return responder(usuario);
	}

	static String normalizarCorreo(String email) {
		return email.replace(" ", "").toLowerCase(Locale.ROOT);
	}

	private RespuestaAuth responder(Usuario usuario) {
		String token = jwtServicio.generar(usuario.getEmail(), usuario.getRol(), usuario.getSalon().getId());
		return new RespuestaAuth(token, "Bearer", jwtServicio.getExpiraEnMinutos(), usuario.getId(),
				usuario.getNombre(), usuario.getRol(), usuario.getSalon().getId(), usuario.getSalon().getSlug());
	}

}
