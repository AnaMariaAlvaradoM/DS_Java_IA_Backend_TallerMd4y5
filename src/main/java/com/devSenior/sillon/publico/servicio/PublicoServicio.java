package com.devSenior.sillon.publico.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.cita.dto.CitaRespuesta;
import com.devSenior.sillon.cita.dto.SolicitudCita;
import com.devSenior.sillon.cita.entidad.EstadoCita;
import com.devSenior.sillon.cita.reglas.ReglasAgenda;
import com.devSenior.sillon.cita.repositorio.CitaRepositorio;
import com.devSenior.sillon.cita.servicio.CitaServicio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.publico.dto.DisponibilidadRespuesta;
import com.devSenior.sillon.publico.dto.EstilistaPublico;
import com.devSenior.sillon.publico.dto.SalonPublico;
import com.devSenior.sillon.publico.dto.ServicioPublico;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class PublicoServicio {

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private final SalonRepositorio salonRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final CitaRepositorio citaRepositorio;
	private final CitaServicio citaServicio;
	private final ReglasAgenda reglasAgenda;

	public PublicoServicio(SalonRepositorio salonRepositorio, ServicioRepositorio servicioRepositorio,
			UsuarioRepositorio usuarioRepositorio, CitaRepositorio citaRepositorio, CitaServicio citaServicio,
			ReglasAgenda reglasAgenda) {
		this.salonRepositorio = salonRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.citaRepositorio = citaRepositorio;
		this.citaServicio = citaServicio;
		this.reglasAgenda = reglasAgenda;
	}

	@Transactional(readOnly = true)
	public SalonPublico catalogo(String slug) {
		Salon salon = salonPorSlug(slug);
		List<ServicioPublico> servicios = servicioRepositorio.findBySalon_IdAndActivoOrderByIdAsc(salon.getId(), true)
				.stream()
				.map(servicio -> new ServicioPublico(servicio.getId(), servicio.getNombre(),
						servicio.getDuracionMinutos(), servicio.getPrecio()))
				.toList();
		List<EstilistaPublico> estilistas = usuarioRepositorio
				.findBySalon_IdAndRolOrderByIdAsc(salon.getId(), Rol.ESTILISTA)
				.stream()
				.filter(Usuario::isActivo)
				.map(estilista -> new EstilistaPublico(estilista.getId(), estilista.getNombre(),
						estilista.getServicios().stream().filter(Servicio::isActivo).map(Servicio::getId).toList()))
				.toList();
		return new SalonPublico(salon.getNombre(), salon.getSlug(), salon.getCiudad(), salon.getDireccion(),
				salon.getTelefono(), horario(salon.getConfiguracion()), servicios, estilistas);
	}

	@Transactional(readOnly = true)
	public DisponibilidadRespuesta disponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
		Salon salon = salonPorSlug(slug);
		Servicio servicio = servicioRepositorio.findByIdAndSalon_Id(servicioId, salon.getId())
				.filter(Servicio::isActivo)
				.orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
		Usuario estilista = usuarioRepositorio.findByIdAndSalon_Id(estilistaId, salon.getId())
				.filter(usuario -> usuario.isActivo() && usuario.getRol() == Rol.ESTILISTA)
				.orElseThrow(() -> new NoEncontradoException("El estilista no existe"));
		if (!estilista.ofreceServicio(servicio.getId())) {
			throw new ConflictoException("El estilista no ofrece el servicio");
		}
		List<LocalDateTime[]> ocupados = citaRepositorio
				.findByEstilista_IdAndSalon_IdAndEstadoIn(estilista.getId(), salon.getId(),
						List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA))
				.stream()
				.map(cita -> new LocalDateTime[] { cita.getInicio(), cita.getFin() })
				.toList();
		return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
				servicio.getDuracionMinutos(),
				reglasAgenda.horariosLibres(salon.getConfiguracion(), fecha, servicio.getDuracionMinutos(), ocupados));
	}

	public CitaRespuesta reservar(String slug, SolicitudCita solicitud) {
		Salon salon = salonRepositorio.findBySlug(slug)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
		return citaServicio.crearEnLinea(salon.getId(), solicitud);
	}

	private Salon salonPorSlug(String slug) {
		return salonRepositorio.findBySlug(slug).orElseThrow(() -> new NoEncontradoException("El salón no existe"));
	}

	private String horario(ConfiguracionSalon configuracion) {
		String dias = configuracion.isAbreDomingos() ? "lunes a domingo" : "lunes a sábado";
		return configuracion.getHoraApertura().format(HORA) + " a " + configuracion.getHoraCierre().format(HORA) + ", "
				+ dias;
	}

}
