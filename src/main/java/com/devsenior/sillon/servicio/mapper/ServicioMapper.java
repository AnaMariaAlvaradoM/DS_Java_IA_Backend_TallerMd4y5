package com.devsenior.sillon.servicio.mapper;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.servicio.dto.RespuestaServicio;
import com.devsenior.sillon.servicio.modelo.Servicio;

@Component
public class ServicioMapper {

	public RespuestaServicio aRespuesta(Servicio servicio) {
		return new RespuestaServicio(
				servicio.getId(),
				servicio.getNombre(),
				servicio.getDescripcion(),
				servicio.getDuracionMinutos(),
				servicio.getPrecio(),
				servicio.isActivo());
	}

}
