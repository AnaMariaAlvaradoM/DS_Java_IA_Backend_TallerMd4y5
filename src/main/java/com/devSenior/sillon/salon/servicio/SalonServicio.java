package com.devSenior.sillon.salon.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.salon.dto.ConfiguracionRespuesta;
import com.devSenior.sillon.salon.dto.SalonRespuesta;
import com.devSenior.sillon.salon.dto.SolicitudConfiguracion;
import com.devSenior.sillon.salon.dto.SolicitudSalon;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class SalonServicio {

	private final SalonRepositorio salonRepositorio;

	public SalonServicio(SalonRepositorio salonRepositorio) {
		this.salonRepositorio = salonRepositorio;
	}

	@Transactional(readOnly = true)
	public SalonRespuesta obtener(Long salonId) {
		return aRespuesta(buscar(salonId));
	}

	@Transactional(readOnly = true)
	public List<SalonRespuesta> listarTodos() {
		return salonRepositorio.findAll().stream()
				.map(this::aRespuesta)
				.toList();
	}

	@Transactional
	public SalonRespuesta actualizarConfiguracion(Long salonId, SolicitudConfiguracion solicitud) {
		if (!solicitud.horaApertura().isBefore(solicitud.horaCierre())) {
			throw new ConflictoException("La hora de apertura debe ser anterior a la de cierre");
		}
		Salon salon = buscar(salonId);
		ConfiguracionSalon configuracion = salon.getConfiguracion() == null
				? new ConfiguracionSalon()
				: salon.getConfiguracion();
		configuracion.setHoraApertura(solicitud.horaApertura());
		configuracion.setHoraCierre(solicitud.horaCierre());
		configuracion.setAbreDomingos(solicitud.abreDomingos());
		configuracion.setIntervaloMinutos(solicitud.intervaloMinutos());
		salon.setConfiguracion(configuracion);
		return aRespuesta(salonRepositorio.save(salon));
	}

	@Transactional
	public SalonRespuesta actualizarDatos(Long salonId, SolicitudSalon solicitud) {
		Salon salon = buscar(salonId);
		salon.setNombre(solicitud.nombre().trim());
		salon.setCiudad(solicitud.ciudad().trim());
		salon.setDireccion(solicitud.direccion().trim());
		salon.setTelefono(solicitud.telefono().trim());
		return aRespuesta(salonRepositorio.save(salon));
	}

	private Salon buscar(Long salonId) {
		return salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
	}

	private SalonRespuesta aRespuesta(Salon salon) {
		ConfiguracionSalon configuracion = salon.getConfiguracion();
		return new SalonRespuesta(
				salon.getId(),
				salon.getNombre(),
				salon.getSlug(),
				salon.getCiudad(),
				salon.getDireccion(),
				salon.getTelefono(),
				salon.getPlan(),
				salon.getCreditosIa(),
				new ConfiguracionRespuesta(
						configuracion.getHoraApertura(),
						configuracion.getHoraCierre(),
						configuracion.isAbreDomingos(),
						configuracion.getIntervaloMinutos()),
				salon.getFechaCreacion());
	}

}
