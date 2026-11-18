package com.devSenior.sillon.chat.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.chat.dto.SolicitudRespuesta;
import com.devSenior.sillon.chat.entidad.EstadoSolicitud;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.devSenior.sillon.chat.entidad.SolicitudContacto;
import com.devSenior.sillon.chat.repositorio.SolicitudContactoRepositorio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class SolicitudContactoServicio {

	static final int MENSAJES_MAXIMO = 2000;

	private final SolicitudContactoRepositorio solicitudRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final Clock reloj;

	public SolicitudContactoServicio(SolicitudContactoRepositorio solicitudRepositorio,
			SalonRepositorio salonRepositorio, Clock reloj) {
		this.solicitudRepositorio = solicitudRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.reloj = reloj;
	}

	@Transactional
	public Long crear(Long salonId, String clienteNombre, String clienteContacto, String mensajes,
			MotivoSolicitud motivo) {
		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
		SolicitudContacto solicitud = new SolicitudContacto();
		solicitud.setClienteNombre(clienteNombre);
		solicitud.setClienteContacto(clienteContacto);
		solicitud.setMensajes(limitar(mensajes));
		solicitud.setMotivo(motivo);
		solicitud.setEstado(EstadoSolicitud.PENDIENTE);
		solicitud.setFechaCreacion(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
		solicitud.setSalon(salon);
		return solicitudRepositorio.save(solicitud).getId();
	}

	@Transactional(readOnly = true)
	public List<SolicitudRespuesta> listar(Long salonId, EstadoSolicitud estado) {
		List<SolicitudContacto> solicitudes = estado == null
				? solicitudRepositorio.findBySalon_IdOrderByFechaCreacionDescIdDesc(salonId)
				: solicitudRepositorio.findBySalon_IdAndEstadoOrderByFechaCreacionDescIdDesc(salonId, estado);
		return solicitudes.stream().map(this::aRespuesta).toList();
	}

	@Transactional
	public SolicitudRespuesta atender(Long salonId, Long id) {
		SolicitudContacto solicitud = solicitudRepositorio.findByIdAndSalon_Id(id, salonId)
				.orElseThrow(() -> new NoEncontradoException("La solicitud no existe"));
		if (solicitud.getEstado() == EstadoSolicitud.ATENDIDA) {
			throw new ConflictoException("La solicitud ya estaba atendida");
		}
		solicitud.setEstado(EstadoSolicitud.ATENDIDA);
		return aRespuesta(solicitudRepositorio.saveAndFlush(solicitud));
	}

	private SolicitudRespuesta aRespuesta(SolicitudContacto solicitud) {
		return new SolicitudRespuesta(solicitud.getId(), solicitud.getClienteNombre(), solicitud.getClienteContacto(),
				solicitud.getMensajes(), solicitud.getMotivo(), solicitud.getEstado(), solicitud.getFechaCreacion());
	}

	private String limitar(String mensajes) {
		if (mensajes == null) {
			return "";
		}
		return mensajes.length() <= MENSAJES_MAXIMO ? mensajes : mensajes.substring(0, MENSAJES_MAXIMO);
	}

}
