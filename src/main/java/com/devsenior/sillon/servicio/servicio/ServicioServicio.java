package com.devsenior.sillon.servicio.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.dto.RespuestaServicio;
import com.devsenior.sillon.servicio.dto.SolicitudServicio;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;

@Service
public class ServicioServicio {

	private final ServicioRepositorio servicioRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final ServicioMapper servicioMapper;

	public ServicioServicio(
			ServicioRepositorio servicioRepositorio,
			SalonRepositorio salonRepositorio,
			ServicioMapper servicioMapper) {

		this.servicioRepositorio = servicioRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.servicioMapper = servicioMapper;
	}

	/**
	 * Crea un servicio en el catálogo del salón autenticado.
	 */
	@Transactional
	public RespuestaServicio crear(Long salonId, SolicitudServicio solicitud) {
		Salon salon = salonRepositorio.findById(salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Salón no encontrado."));

		Servicio servicio = new Servicio();
		servicio.setNombre(solicitud.nombre());
		servicio.setDescripcion(solicitud.descripcion());
		servicio.setDuracionMinutos(solicitud.duracionMinutos());
		servicio.setPrecio(solicitud.precio());
		servicio.setActivo(solicitud.activo() == null || solicitud.activo());
		servicio.setSalon(salon);

		Servicio guardado = servicioRepositorio.save(servicio);
		return servicioMapper.aRespuesta(guardado);
	}

	/**
	 * Lista los servicios pertenecientes al salón del usuario autenticado.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaServicio> listar(Long salonId) {
		return servicioRepositorio.findAllBySalonIdOrderByIdAsc(salonId)
				.stream()
				.map(servicioMapper::aRespuesta)
				.toList();
	}

	/**
	 * Actualiza un servicio existente validando pertenencia al salón.
	 */
	@Transactional
	public RespuestaServicio actualizar(Long salonId, Long servicioId, SolicitudServicio solicitud) {
		Servicio servicio = servicioRepositorio.findByIdAndSalonId(servicioId, salonId)
				.orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado."));

		servicio.setNombre(solicitud.nombre());
		servicio.setDescripcion(solicitud.descripcion());
		servicio.setDuracionMinutos(solicitud.duracionMinutos());
		servicio.setPrecio(solicitud.precio());
		if (solicitud.activo() != null) {
			servicio.setActivo(solicitud.activo());
		}

		Servicio actualizado = servicioRepositorio.save(servicio);
		return servicioMapper.aRespuesta(actualizado);
	}

}
