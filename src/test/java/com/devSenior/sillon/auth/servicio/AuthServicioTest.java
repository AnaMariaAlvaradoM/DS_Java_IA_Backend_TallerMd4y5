package com.devSenior.sillon.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devSenior.sillon.auth.dto.RespuestaAuth;
import com.devSenior.sillon.auth.dto.SolicitudLogin;
import com.devSenior.sillon.auth.dto.SolicitudRegistro;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoAutenticadoException;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.salon.servicio.GeneradorSlug;
import com.devSenior.sillon.seguridad.JwtServicio;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class AuthServicioTest {

	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneId.of("America/Bogota"));

	@Mock
	private UsuarioRepositorio usuarioRepositorio;

	@Mock
	private SalonRepositorio salonRepositorio;

	@Mock
	private GeneradorSlug generadorSlug;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private AuthServicio authServicio;

	@BeforeEach
	void preparar() {
		JwtServicio jwtServicio = new JwtServicio("secreto-de-prueba-con-32-caracteres", 480, RELOJ);
		authServicio = new AuthServicio(usuarioRepositorio, salonRepositorio, generadorSlug, passwordEncoder,
				jwtServicio, RELOJ, 10);
	}

	@Test
	void registraSalonGratisConDiezCreditosYDueno() {
		when(usuarioRepositorio.existsByEmail("ana@sillon.test")).thenReturn(false);
		when(generadorSlug.generar("Sillón Centro")).thenReturn("sillon-centro");
		when(salonRepositorio.save(any(Salon.class))).thenAnswer(invocacion -> {
			Salon salon = invocacion.getArgument(0);
			salon.setId(4L);
			return salon;
		});
		when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(invocacion -> {
			Usuario usuario = invocacion.getArgument(0);
			usuario.setId(9L);
			return usuario;
		});

		RespuestaAuth respuesta = authServicio.registrar(new SolicitudRegistro(
				"Sillón Centro", "Bogotá", "Calle 1", "3001234567", "Ana", " Ana@Sillon.TEST ", "clave1234"));

		ArgumentCaptor<Salon> salonCaptor = ArgumentCaptor.forClass(Salon.class);
		verify(salonRepositorio).save(salonCaptor.capture());
		Salon salon = salonCaptor.getValue();
		assertEquals(Plan.GRATIS, salon.getPlan());
		assertEquals(10, salon.getCreditosIa());
		assertEquals("sillon-centro", salon.getSlug());
		assertEquals(LocalTime.of(8, 0), salon.getConfiguracion().getHoraApertura());
		assertEquals(LocalTime.of(19, 0), salon.getConfiguracion().getHoraCierre());
		assertEquals(false, salon.getConfiguracion().isAbreDomingos());
		assertEquals(30, salon.getConfiguracion().getIntervaloMinutos());

		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepositorio).save(usuarioCaptor.capture());
		Usuario dueno = usuarioCaptor.getValue();
		assertEquals("ana@sillon.test", dueno.getEmail());
		assertEquals(Rol.DUENO, dueno.getRol());
		assertTrue(dueno.getPassword().startsWith("$2"));
		assertTrue(passwordEncoder.matches("clave1234", dueno.getPassword()));

		assertEquals("Bearer", respuesta.tipo());
		assertEquals(480, respuesta.expiraEnMinutos());
		assertEquals(9L, respuesta.usuarioId());
		assertEquals(4L, respuesta.salonId());
		assertEquals("sillon-centro", respuesta.salonSlug());
		assertEquals(Rol.DUENO, respuesta.rol());
	}

	@Test
	void correoRepetidoResponde409SinCrearSalon() {
		when(usuarioRepositorio.existsByEmail("ana@sillon.test")).thenReturn(true);

		assertThrows(ConflictoException.class, () -> authServicio.registrar(solicitud()));
		verify(salonRepositorio, never()).save(any());
	}

	@Test
	void loginIncorrectoUsaElMismoMensajeSiFallaCorreoClaveOCuentaInactiva() {
		when(usuarioRepositorio.findByEmail("ana@sillon.test")).thenReturn(Optional.empty());
		NoAutenticadoException sinCorreo = assertThrows(NoAutenticadoException.class,
				() -> authServicio.login(new SolicitudLogin("ana@sillon.test", "clave1234")));

		Usuario inactivo = usuario("clave1234", false);
		when(usuarioRepositorio.findByEmail("ana@sillon.test")).thenReturn(Optional.of(inactivo));
		NoAutenticadoException cuentaInactiva = assertThrows(NoAutenticadoException.class,
				() -> authServicio.login(new SolicitudLogin("ana@sillon.test", "clave1234")));

		when(usuarioRepositorio.findByEmail("ana@sillon.test")).thenReturn(Optional.of(usuario("clave1234", true)));
		NoAutenticadoException claveMala = assertThrows(NoAutenticadoException.class,
				() -> authServicio.login(new SolicitudLogin("ana@sillon.test", "otra-clave")));

		assertEquals(sinCorreo.getMessage(), cuentaInactiva.getMessage());
		assertEquals(sinCorreo.getMessage(), claveMala.getMessage());
	}

	private SolicitudRegistro solicitud() {
		return new SolicitudRegistro("Sillón Centro", "Bogotá", "Calle 1", "3001234567", "Ana", "ana@sillon.test",
				"clave1234");
	}

	private Usuario usuario(String clave, boolean activo) {
		Salon salon = new Salon();
		salon.setId(4L);
		salon.setSlug("sillon-centro");
		Usuario usuario = new Usuario();
		usuario.setId(9L);
		usuario.setNombre("Ana");
		usuario.setEmail("ana@sillon.test");
		usuario.setPassword(passwordEncoder.encode(clave));
		usuario.setRol(Rol.DUENO);
		usuario.setActivo(activo);
		usuario.setSalon(salon);
		return usuario;
	}

}
