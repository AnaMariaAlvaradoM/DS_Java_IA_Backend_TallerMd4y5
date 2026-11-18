package com.devSenior.sillon.cita.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.cita.entidad.Cita;
import com.devSenior.sillon.cita.repositorio.CitaRepositorio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;

@Service
public class LecturaCitaServicio {

	private final CitaRepositorio citaRepositorio;

	public LecturaCitaServicio(CitaRepositorio citaRepositorio) {
		this.citaRepositorio = citaRepositorio;
	}

	@Transactional(readOnly = true)
	public DatosRecordatorio paraRecordatorio(Long salonId, Long citaId) {
		Cita cita = citaRepositorio.findByIdAndSalon_Id(citaId, salonId)
				.orElseThrow(() -> new NoEncontradoException("La cita no existe"));
		if (!cita.getEstado().ocupaAgenda()) {
			throw new ConflictoException("Solo se recuerda una cita pendiente o confirmada");
		}
		String datos = FormatoRecordatorio.datos(
				cita.getServicio().getNombre(),
				cita.getServicio().getDuracionMinutos(),
				cita.getEstilista().getNombre(),
				cita.getInicio().toLocalDate(),
				cita.getInicio().toLocalTime(),
				cita.getPrecio(),
				cita.getSalon().getDireccion());
		return new DatosRecordatorio(cita.getId(), salonId, cita.getSalon().getNombre(), cita.getCliente().getNombre(),
				cita.getCliente().getTelefono(), datos, cita.getInicio());
	}

}
