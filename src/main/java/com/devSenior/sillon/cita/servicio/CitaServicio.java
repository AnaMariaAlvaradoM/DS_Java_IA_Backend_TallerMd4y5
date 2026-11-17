package com.devSenior.sillon.cita.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.ProhibidoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class CitaServicio {

	private final CitaRepositorio citaRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final ClienteServicio clienteServicio;
	private final ReglasAgenda reglasAgenda;
	private final Clock reloj;

	public CitaServicio(CitaRepositorio citaRepositorio, SalonRepositorio salonRepositorio,
			ServicioRepositorio servicioRepositorio, UsuarioRepositorio usuarioRepositorio,
			ClienteServicio clienteServicio, ReglasAgenda reglasAgenda, Clock reloj) {
		this.citaRepositorio = citaRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.clienteServicio = clienteServicio;
		this.reglasAgenda = reglasAgenda;
		this.reloj = reloj;
	}

	@Transactional
	public CitaRespuesta crearPorSalon(Long salonId, SolicitudCita solicitud) {
		return aRespuesta(agendar(salonId, solicitud, EstadoCita.PENDIENTE, OrigenCita.SALON));
	}

	@Transactional
	public CitaRespuesta crearEnLinea(Long salonId, SolicitudCita solicitud) {
		return aRespuesta(agendar(salonId, solicitud, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA));
	}

	@Transactional(readOnly = true)
	public List<CitaRespuesta> listar(UsuarioPrincipal principal, LocalDate desde, LocalDate hasta, EstadoCita estado,
			Long estilistaId) {
		Long filtroEstilista = principal.getRol() == Rol.ESTILISTA ? principal.getId() : estilistaId;
		LocalDateTime inicioRango = desde == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : desde.atStartOfDay();
		LocalDateTime finRango = hasta == null ? LocalDateTime.of(2100, 1, 1, 0, 0) : hasta.plusDays(1).atStartOfDay();
		return citaRepositorio
				.findBySalon_IdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(principal.getSalonId(),
						inicioRango, finRango)
				.stream()
				.filter(cita -> estado == null || cita.getEstado() == estado)
				.filter(cita -> filtroEstilista == null || filtroEstilista.equals(cita.getEstilista().getId()))
				.map(this::aRespuesta)
				.toList();
	}

	@Transactional(readOnly = true)
	public CitaRespuesta obtener(UsuarioPrincipal principal, Long id) {
		Cita cita = citaDelSalon(principal.getSalonId(), id);
		if (principal.getRol() == Rol.ESTILISTA && !principal.getId().equals(cita.getEstilista().getId())) {
			throw new NoEncontradoException("La cita no existe");
		}
		return aRespuesta(cita);
	}

	@Transactional
	public CitaRespuesta cambiarEstado(UsuarioPrincipal principal, Long id, EstadoCita destino) {
		Cita cita = citaDelSalon(principal.getSalonId(), id);
		if (principal.getRol() == Rol.ESTILISTA && !principal.getId().equals(cita.getEstilista().getId())) {
			throw new NoEncontradoException("La cita no existe");
		}
		if (principal.getRol() == Rol.ESTILISTA && destino != EstadoCita.COMPLETADA && destino != EstadoCita.NO_ASISTIO) {
			throw new ProhibidoException("El estilista solo puede completar o marcar inasistencia");
		}
		if (!cita.getEstado().puedeCambiarA(destino)) {
			throw new ConflictoException("La transición de estado no está permitida");
		}
		cita.setEstado(destino);
		cita.setFechaActualizacion(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
		return aRespuesta(citaRepositorio.saveAndFlush(cita));
	}

	private Cita agendar(Long salonId, SolicitudCita solicitud, EstadoCita estado, OrigenCita origen) {
		Servicio servicio = servicioRepositorio.findByIdAndSalon_Id(solicitud.servicioId(), salonId)
				.filter(Servicio::isActivo)
				.orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
		Usuario estilista = usuarioRepositorio.findByIdAndSalon_Id(solicitud.estilistaId(), salonId)
				.filter(usuario -> usuario.isActivo() && usuario.getRol() == Rol.ESTILISTA)
				.orElseThrow(() -> new NoEncontradoException("El estilista no existe"));
		if (!estilista.ofreceServicio(servicio.getId())) {
			throw new ConflictoException("El estilista no ofrece el servicio");
		}
		LocalDateTime fin = solicitud.inicio().plusMinutes(servicio.getDuracionMinutos());
		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
		reglasAgenda.validar(salon.getConfiguracion(), solicitud.inicio(), fin);
		List<Cita> ocupadas = citaRepositorio.findByEstilista_IdAndSalon_IdAndEstadoIn(estilista.getId(), salonId,
				List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));
		for (Cita ocupada : ocupadas) {
			if (ocupada.getEstado().ocupaAgenda()
					&& reglasAgenda.seCruzan(solicitud.inicio(), fin, ocupada.getInicio(), ocupada.getFin())) {
				throw new ConflictoException("La cita se cruza con otra del estilista");
			}
		}
		Cliente cliente = clienteServicio.buscarOCrear(salonId, solicitud.clienteNombre(), solicitud.clienteTelefono());
		LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
		Cita cita = new Cita();
		cita.setInicio(solicitud.inicio());
		cita.setFin(fin);
		cita.setPrecio(servicio.getPrecio());
		cita.setEstado(estado);
		cita.setOrigen(origen);
		cita.setNota(solicitud.nota());
		cita.setFechaCreacion(ahora);
		cita.setFechaActualizacion(ahora);
		cita.setSalon(salon);
		cita.setCliente(cliente);
		cita.setEstilista(estilista);
		cita.setServicio(servicio);
		return citaRepositorio.save(cita);
	}

	private Cita citaDelSalon(Long salonId, Long id) {
		return citaRepositorio.findByIdAndSalon_Id(id, salonId)
				.orElseThrow(() -> new NoEncontradoException("La cita no existe"));
	}

	private CitaRespuesta aRespuesta(Cita cita) {
		return new CitaRespuesta(
				cita.getId(),
				cita.getInicio(),
				cita.getFin(),
				cita.getEstado(),
				cita.getOrigen(),
				cita.getPrecio(),
				cita.getServicio().getId(),
				cita.getServicio().getNombre(),
				cita.getEstilista().getId(),
				cita.getEstilista().getNombre(),
				cita.getCliente().getNombre(),
				cita.getCliente().getTelefono(),
				cita.getNota(),
				cita.getFechaCreacion(),
				cita.getFechaActualizacion());
	}

}
