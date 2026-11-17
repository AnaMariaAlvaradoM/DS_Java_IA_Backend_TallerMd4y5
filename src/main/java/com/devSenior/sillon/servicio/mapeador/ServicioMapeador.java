package com.devSenior.sillon.servicio.mapeador;

import com.devSenior.sillon.servicio.dto.ServicioRespuesta;
import com.devSenior.sillon.servicio.entidad.Servicio;

public final class ServicioMapeador {

	private ServicioMapeador() {
	}

	public static ServicioRespuesta aRespuesta(Servicio servicio) {
		return new ServicioRespuesta(
				servicio.getId(),
				servicio.getNombre(),
				servicio.getDescripcion(),
				servicio.getDuracionMinutos(),
				servicio.getPrecio(),
				servicio.isActivo());
	}

}
