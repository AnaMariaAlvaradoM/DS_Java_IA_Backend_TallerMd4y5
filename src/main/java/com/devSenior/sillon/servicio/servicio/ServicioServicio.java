package com.devSenior.sillon.servicio.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.servicio.dto.ServicioRespuesta;
import com.devSenior.sillon.servicio.dto.SolicitudServicio;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.mapeador.ServicioMapeador;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;

@Service
public class ServicioServicio {

	private final ServicioRepositorio servicioRepositorio;
	private final SalonRepositorio salonRepositorio;

	public ServicioServicio(ServicioRepositorio servicioRepositorio, SalonRepositorio salonRepositorio) {
		this.servicioRepositorio = servicioRepositorio;
		this.salonRepositorio = salonRepositorio;
	}

	@Transactional
	public ServicioRespuesta crear(Long salonId, SolicitudServicio solicitud) {
		Servicio servicio = new Servicio();
		servicio.setSalon(buscarSalon(salonId));
		copiar(servicio, solicitud);
		return ServicioMapeador.aRespuesta(servicioRepositorio.save(servicio));
	}

	@Transactional(readOnly = true)
	public List<ServicioRespuesta> listar(Long salonId, Boolean activo) {
		List<Servicio> servicios = activo == null
				? servicioRepositorio.findBySalon_IdOrderByIdAsc(salonId)
				: servicioRepositorio.findBySalon_IdAndActivoOrderByIdAsc(salonId, activo);
		return servicios.stream().map(ServicioMapeador::aRespuesta).toList();
	}

	@Transactional
	public ServicioRespuesta actualizar(Long salonId, Long servicioId, SolicitudServicio solicitud) {
		Servicio servicio = servicioRepositorio.findByIdAndSalon_Id(servicioId, salonId)
				.orElseThrow(() -> new NoEncontradoException("El servicio no existe"));
		copiar(servicio, solicitud);
		return ServicioMapeador.aRespuesta(servicioRepositorio.save(servicio));
	}

	private void copiar(Servicio servicio, SolicitudServicio solicitud) {
		servicio.setNombre(solicitud.nombre().trim());
		servicio.setDescripcion(solicitud.descripcion() == null ? null : solicitud.descripcion().trim());
		servicio.setDuracionMinutos(solicitud.duracionMinutos());
		servicio.setPrecio(solicitud.precio());
		servicio.setActivo(solicitud.activo() == null || solicitud.activo());
	}

	private Salon buscarSalon(Long salonId) {
		return salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
	}

}
