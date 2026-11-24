package com.devsenior.sillon.cita.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCambiarEstadoCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.mapper.CitaMapper;
import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.modelo.OrigenCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.cliente.repositorio.ClienteRepositorio;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoPermitida;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class CitaServicio {

	private static final List<EstadoCita> ESTADOS_QUE_OCUPAN_AGENDA = List.of(
			EstadoCita.PENDIENTE,
			EstadoCita.CONFIRMADA);

	private final SalonRepositorio salonRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final CitaRepositorio citaRepositorio;
	private final ReglasAgenda reglasAgenda;
	private final CitaMapper citaMapper;
	private final Clock reloj;

	public CitaServicio(
			SalonRepositorio salonRepositorio,
			ServicioRepositorio servicioRepositorio,
			UsuarioRepositorio usuarioRepositorio,
			ClienteRepositorio clienteRepositorio,
			CitaRepositorio citaRepositorio,
			ReglasAgenda reglasAgenda,
			CitaMapper citaMapper,
			Clock reloj) {

		this.salonRepositorio = salonRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.citaRepositorio = citaRepositorio;
		this.reglasAgenda = reglasAgenda;
		this.citaMapper = citaMapper;
		this.reloj = reloj;
	}

	/**
	 * Crea una cita desde la administración del salón en estado CONFIRMADA y origen SALON.
	 */
	@Transactional
	public RespuestaCita crear(Long salonId, SolicitudCrearCita solicitud) {
		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Salón no encontrado."));

		return agendarEnSalon(salon, solicitud, EstadoCita.CONFIRMADA, OrigenCita.SALON);
	}

	/**
	 * Lista las citas de un día ordenadas por hora. El dueño consulta todo el salón;
	 * el estilista únicamente sus propias citas.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaCita> listarPorFecha(
			Long salonId,
			Long usuarioId,
			Rol rol,
			LocalDate fecha) {

		LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now(reloj);
		LocalDateTime inicioDia = fechaConsulta.atStartOfDay();
		LocalDateTime finDia = fechaConsulta.plusDays(1).atStartOfDay();

		List<Cita> citas = rol == Rol.ESTILISTA
				? citaRepositorio.buscarPorSalonEstilistaYRango(salonId, usuarioId, inicioDia, finDia)
				: citaRepositorio.buscarPorSalonYRango(salonId, inicioDia, finDia);

		return citas.stream()
				.map(citaMapper::aRespuesta)
				.toList();
	}

	/**
	 * Cambia el estado de una cita validando pertenencia al salón, permisos por rol,
	 * transiciones permitidas y ejecutando saveAndFlush para actualizar la fecha de auditoría.
	 */
	@Transactional
	public RespuestaCita cambiarEstado(
			Long salonId,
			Long usuarioId,
			Rol rol,
			Long citaId,
			SolicitudCambiarEstadoCita solicitud) {

		Cita cita = citaRepositorio.buscarPorIdYSalon(citaId, salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Cita no encontrada."));

		if (rol == Rol.ESTILISTA) {
			if (!cita.getEstilista().getId().equals(usuarioId)) {
				throw new ExcepcionRecursoNoEncontrado("Cita no encontrada.");
			}
			if (solicitud.estado() != EstadoCita.COMPLETADA
					&& solicitud.estado() != EstadoCita.NO_ASISTIO) {
				throw new ExcepcionNoPermitida(
						"El estilista solo puede cambiar sus citas a COMPLETADA o NO_ASISTIO.");
			}
		}

		if (!cita.getEstado().puedeTransicionarA(solicitud.estado())) {
			throw new ExcepcionConflicto(
					"No se permite cambiar el estado de "
							+ cita.getEstado()
							+ " a "
							+ solicitud.estado()
							+ ".");
		}

		cita.setEstado(solicitud.estado());
		Cita actualizada = citaRepositorio.saveAndFlush(cita);
		return citaMapper.aRespuesta(actualizada);
	}

	/**
	 * Flujo único y privado de validación y persistencia de citas para un salón.
	 * En la Fase 5 las reservas públicas resolverán el salón por slug y delegarán aquí
	 * con EstadoCita.PENDIENTE y OrigenCita.EN_LINEA.
	 */
	private RespuestaCita agendarEnSalon(
			Salon salon,
			SolicitudCrearCita solicitud,
			EstadoCita estadoInicial,
			OrigenCita origenInicial) {

		Long salonId = salon.getId();

		// 1. Verificar que el servicio exista en el salón y esté activo
		Servicio servicio = servicioRepositorio.findByIdAndSalonId(solicitud.servicioId(), salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado."));

		if (!servicio.isActivo()) {
			throw new ExcepcionConflicto("El servicio seleccionado no está activo.");
		}

		// 2. Verificar que el estilista exista en el salón, esté activo y ofrezca el servicio
		Usuario estilista = usuarioRepositorio.buscarEstilistaConServiciosPorId(
				solicitud.estilistaId(),
				salonId,
				Rol.ESTILISTA)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Estilista no encontrado."));

		if (!estilista.isActivo()) {
			throw new ExcepcionConflicto("El estilista seleccionado no está activo.");
		}

		boolean ofreceServicio = estilista.getServicios().stream()
				.anyMatch(ofrecido -> ofrecido.getId().equals(servicio.getId()));
		if (!ofreceServicio) {
			throw new ExcepcionConflicto("El estilista no ofrece el servicio seleccionado.");
		}

		// 3. Calcular la hora de finalización
		LocalDateTime inicio = solicitud.inicio();
		LocalDateTime fin = reglasAgenda.calcularFin(inicio, servicio.getDuracionMinutos());

		// 4. Rechazar citas en el pasado
		reglasAgenda.validarNoEsEnElPasado(inicio);

		// 5. Validar el horario del salón
		reglasAgenda.validarHorarioSalon(inicio, fin, salon.getConfiguracion());

		// 6. Verificar que no exista cruce con citas que ocupan agenda
		List<Cita> citasActivas = citaRepositorio.buscarCitasActivasEstilistaEnRango(
				salonId,
				estilista.getId(),
				ESTADOS_QUE_OCUPAN_AGENDA,
				inicio,
				fin);
		reglasAgenda.validarSinCruces(inicio, fin, citasActivas);

		// 7. Buscar o crear el cliente, copiar el precio y guardar la cita
		Cliente cliente = buscarOCrearCliente(
				salon,
				solicitud.clienteNombre().trim(),
				normalizarTelefono(solicitud.clienteTelefono()));

		Cita cita = new Cita();
		cita.setSalon(salon);
		cita.setCliente(cliente);
		cita.setEstilista(estilista);
		cita.setServicio(servicio);
		cita.setInicio(inicio);
		cita.setFin(fin);
		cita.setPrecio(servicio.getPrecio());
		cita.setEstado(estadoInicial);
		cita.setOrigen(origenInicial);
		cita.setNota(solicitud.nota());

		Cita guardada = citaRepositorio.saveAndFlush(cita);
		return citaMapper.aRespuesta(guardada);
	}

	private Cliente buscarOCrearCliente(Salon salon, String nombre, String telefonoNormalizado) {
		return clienteRepositorio.findBySalonIdAndTelefono(salon.getId(), telefonoNormalizado)
				.orElseGet(() -> crearCliente(salon, nombre, telefonoNormalizado));
	}

	private Cliente crearCliente(Salon salon, String nombre, String telefonoNormalizado) {
		Cliente nuevo = new Cliente();
		nuevo.setNombre(nombre);
		nuevo.setTelefono(telefonoNormalizado);
		nuevo.setSalon(salon);
		try {
			return clienteRepositorio.saveAndFlush(nuevo);
		} catch (DataIntegrityViolationException excepcion) {
			return clienteRepositorio.findBySalonIdAndTelefono(salon.getId(), telefonoNormalizado)
					.orElseThrow(() -> excepcion);
		}
	}

	private String normalizarTelefono(String telefono) {
		String filtrado = telefono.replaceAll("[^0-9+]", "");
		if (filtrado.contains("+")) {
			boolean iniciaConMas = filtrado.startsWith("+");
			filtrado = (iniciaConMas ? "+" : "") + filtrado.replace("+", "");
		}
		int digitos = filtrado.startsWith("+") ? filtrado.length() - 1 : filtrado.length();
		if (digitos < 7 || filtrado.length() > 30) {
			throw new ExcepcionSolicitudInvalida(
					"clienteTelefono: debe contener entre 7 y 30 dígitos válidos.");
		}
		return filtrado;
	}

}
