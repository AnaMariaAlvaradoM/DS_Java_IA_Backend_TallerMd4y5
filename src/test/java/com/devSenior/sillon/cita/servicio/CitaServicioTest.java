package com.devSenior.sillon.cita.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.cita.dto.CitaRespuesta;
import com.devSenior.sillon.cita.dto.SolicitudCita;
import com.devSenior.sillon.cita.entidad.Cita;
import com.devSenior.sillon.cita.entidad.EstadoCita;
import com.devSenior.sillon.cita.entidad.OrigenCita;
import com.devSenior.sillon.cita.reglas.ReglasAgenda;
import com.devSenior.sillon.cita.repositorio.CitaRepositorio;
import com.devSenior.sillon.cliente.entidad.Cliente;
import com.devSenior.sillon.cliente.servicio.ClienteServicio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.ProhibidoException;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class CitaServicioTest {

	private static final ZoneId ZONA = ZoneId.of("America/Bogota");
	private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 18, 10, 0);

	@Mock
	private CitaRepositorio citaRepositorio;
	@Mock
	private SalonRepositorio salonRepositorio;
	@Mock
	private ServicioRepositorio servicioRepositorio;
	@Mock
	private UsuarioRepositorio usuarioRepositorio;
	@Mock
	private ClienteServicio clienteServicio;

	private CitaServicio citaServicio;
	private Salon salon;
	private Servicio servicio;
	private Usuario estilista;
	private Usuario otroEstilista;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(LocalDateTime.of(2026, 11, 17, 10, 0).atZone(ZONA).toInstant(), ZONA);
		citaServicio = new CitaServicio(citaRepositorio, salonRepositorio, servicioRepositorio, usuarioRepositorio,
				clienteServicio, new ReglasAgenda(reloj), reloj);
		salon = new Salon();
		salon.setId(1L);
		salon.setConfiguracion(ConfiguracionSalon.porDefecto());
		servicio = new Servicio();
		servicio.setId(7L);
		servicio.setNombre("Corte");
		servicio.setDuracionMinutos(45);
		servicio.setPrecio(35000);
		servicio.setActivo(true);
		estilista = estilista(3L, "Mateo");
		otroEstilista = estilista(4L, "Sara");
	}

	@Test
	void creaCitaConfirmadaDeSalonConFinYPrecioCopiados() {
		prepararAgendar(List.of());
		CitaRespuesta respuesta = citaServicio.crearPorSalon(1L, solicitud(INICIO));
		assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
		assertEquals(OrigenCita.SALON, respuesta.origen());
		assertEquals(INICIO.plusMinutes(45), respuesta.fin());
		assertEquals(35000, respuesta.precio());
	}

	@Test
	void creaReservaPendienteEnLinea() {
		prepararAgendar(List.of());
		CitaRespuesta respuesta = citaServicio.crearEnLinea(1L, solicitud(INICIO));
		assertEquals(EstadoCita.PENDIENTE, respuesta.estado());
		assertEquals(OrigenCita.EN_LINEA, respuesta.origen());
		assertEquals(INICIO.plusMinutes(45), respuesta.fin());
		assertEquals(35000, respuesta.precio());
	}

	@Test
	void rechazaSolapamientoYAceptaConsecutivaYCancelada() {
		Cita ocupada = cita(INICIO, INICIO.plusMinutes(45), EstadoCita.PENDIENTE);
		prepararAgendar(List.of(ocupada));
		assertThrows(ConflictoException.class, () -> citaServicio.crearPorSalon(1L, solicitud(INICIO)));

		prepararAgendar(List.of(ocupada));
		CitaRespuesta consecutiva = citaServicio.crearPorSalon(1L, solicitud(INICIO.plusHours(1)));
		assertEquals(INICIO.plusHours(1), consecutiva.inicio());

		ocupada.setEstado(EstadoCita.CANCELADA);
		prepararAgendar(List.of(ocupada));
		CitaRespuesta liberada = citaServicio.crearPorSalon(1L, solicitud(INICIO));
		assertEquals(EstadoCita.CONFIRMADA, liberada.estado());
	}

	@Test
	void rechazaDomingoDesalineadoPasadoYMasDe90Dias() {
		prepararAgendar(List.of());
		assertThrows(ConflictoException.class,
				() -> citaServicio.crearPorSalon(1L, solicitud(LocalDateTime.of(2026, 11, 22, 10, 0))));
		assertThrows(FirmaInvalidaException.class,
				() -> citaServicio.crearPorSalon(1L, solicitud(LocalDateTime.of(2026, 11, 18, 10, 15))));
		assertThrows(ConflictoException.class,
				() -> citaServicio.crearPorSalon(1L, solicitud(LocalDateTime.of(2026, 11, 17, 9, 0))));
		assertThrows(FirmaInvalidaException.class,
				() -> citaServicio.crearPorSalon(1L, solicitud(LocalDateTime.of(2027, 6, 1, 10, 0))));
	}

	@Test
	void estilistaSoloVeYCierraSusCitas() {
		Cita propia = cita(INICIO, INICIO.plusMinutes(45), EstadoCita.CONFIRMADA);
		propia.setId(20L);
		propia.setEstilista(estilista);
		Cita ajena = cita(INICIO, INICIO.plusMinutes(45), EstadoCita.CONFIRMADA);
		ajena.setId(21L);
		ajena.setEstilista(otroEstilista);
		when(citaRepositorio.findByIdAndSalon_Id(20L, 1L)).thenReturn(Optional.of(propia));
		when(citaRepositorio.findByIdAndSalon_Id(21L, 1L)).thenReturn(Optional.of(ajena));
		when(citaRepositorio.saveAndFlush(propia)).thenReturn(propia);
		UsuarioPrincipal mateo = principal(3L);

		assertEquals(20L, citaServicio.obtener(mateo, 20L).id());
		assertThrows(NoEncontradoException.class, () -> citaServicio.obtener(mateo, 21L));
		assertThrows(ProhibidoException.class, () -> citaServicio.cambiarEstado(mateo, 20L, EstadoCita.CANCELADA));
		assertEquals(EstadoCita.COMPLETADA, citaServicio.cambiarEstado(mateo, 20L, EstadoCita.COMPLETADA).estado());
		propia.setEstado(EstadoCita.COMPLETADA);
		assertThrows(ConflictoException.class, () -> citaServicio.cambiarEstado(mateo, 20L, EstadoCita.NO_ASISTIO));
	}

	private void prepararAgendar(List<Cita> ocupadas) {
		when(servicioRepositorio.findByIdAndSalon_Id(7L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.findByIdAndSalon_Id(3L, 1L)).thenReturn(Optional.of(estilista));
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		lenient().when(citaRepositorio.findByEstilista_IdAndSalon_IdAndEstadoIn(eq(3L), eq(1L), any())).thenReturn(ocupadas);
		Cliente cliente = new Cliente();
		cliente.setId(9L);
		cliente.setNombre("Ana");
		cliente.setTelefono("3001112233");
		lenient().when(clienteServicio.buscarOCrear(1L, "Ana", "3001112233")).thenReturn(cliente);
		lenient().when(citaRepositorio.save(any(Cita.class))).thenAnswer(invocacion -> {
			Cita cita = invocacion.getArgument(0);
			cita.setId(11L);
			return cita;
		});
	}

	private SolicitudCita solicitud(LocalDateTime inicio) {
		return new SolicitudCita("Ana", "3001112233", 7L, 3L, inicio, null);
	}

	private Usuario estilista(Long id, String nombre) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre(nombre);
		usuario.setRol(Rol.ESTILISTA);
		usuario.setActivo(true);
		usuario.setServicios(Set.of(servicio));
		return usuario;
	}

	private Cita cita(LocalDateTime inicio, LocalDateTime fin, EstadoCita estado) {
		Cita cita = new Cita();
		cita.setInicio(inicio);
		cita.setFin(fin);
		cita.setEstado(estado);
		cita.setEstilista(estilista);
		cita.setServicio(servicio);
		Cliente cliente = new Cliente();
		cliente.setNombre("Ana");
		cliente.setTelefono("3001112233");
		cita.setCliente(cliente);
		return cita;
	}

	private UsuarioPrincipal principal(Long id) {
		return new UsuarioPrincipal(id, 1L, "mateo@sillon.test", "hash", Rol.ESTILISTA, true);
	}

}
