package com.devSenior.sillon.chat.servicio;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.cita.servicio.FormatoRecordatorio;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.publico.servicio.PublicoServicio;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;

@Service
public class ContextoChatServicio {

	private final SalonRepositorio salonRepositorio;
	private final ServicioRepositorio servicioRepositorio;
	private final PublicoServicio publicoServicio;

	public ContextoChatServicio(SalonRepositorio salonRepositorio, ServicioRepositorio servicioRepositorio,
			PublicoServicio publicoServicio) {
		this.salonRepositorio = salonRepositorio;
		this.servicioRepositorio = servicioRepositorio;
		this.publicoServicio = publicoServicio;
	}

	@Transactional(readOnly = true)
	public ContextoChat cargar(String slug) {
		Salon salon = salonRepositorio.findBySlug(slug)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
		String catalogo = servicioRepositorio.findBySalon_IdAndActivoOrderByIdAsc(salon.getId(), true).stream()
				.map(this::linea)
				.collect(Collectors.joining("\n"));
		String horario = publicoServicio.catalogo(slug).horario();
		return new ContextoChat(salon.getId(), salon.getNombre(), salon.getDireccion(), horario, catalogo);
	}

	private String linea(Servicio servicio) {
		return servicio.getNombre() + " — " + servicio.getDuracionMinutos() + " min — "
				+ FormatoRecordatorio.dinero(servicio.getPrecio());
	}

}
