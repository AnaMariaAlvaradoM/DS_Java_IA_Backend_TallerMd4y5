package com.devsenior.sillon.cita.mapper;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.modelo.Cita;

@Component
public class CitaMapper {

	public RespuestaCita aRespuesta(Cita cita) {
		return new RespuestaCita(
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
