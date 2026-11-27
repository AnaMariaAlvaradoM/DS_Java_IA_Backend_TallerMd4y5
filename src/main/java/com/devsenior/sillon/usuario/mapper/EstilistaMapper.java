package com.devsenior.sillon.usuario.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.dto.RespuestaEstilista;
import com.devsenior.sillon.usuario.modelo.Usuario;

@Component
public class EstilistaMapper {

	public RespuestaEstilista aRespuesta(Usuario estilista) {
		List<String> nombresServicios = estilista.getServicios()
				.stream()
				.map(Servicio::getNombre)
				.toList();

		return new RespuestaEstilista(
				estilista.getId(),
				estilista.getNombre(),
				estilista.getEmail(),
				estilista.isActivo(),
				nombresServicios);
	}

}
