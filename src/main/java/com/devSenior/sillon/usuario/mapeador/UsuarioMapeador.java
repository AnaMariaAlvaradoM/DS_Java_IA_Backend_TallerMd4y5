package com.devSenior.sillon.usuario.mapeador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.usuario.dto.EstilistaRespuesta;
import com.devSenior.sillon.usuario.dto.UsuarioRespuesta;
import com.devSenior.sillon.usuario.entidad.Usuario;

public final class UsuarioMapeador {

	private UsuarioMapeador() {
	}

	public static UsuarioRespuesta aRespuesta(Usuario usuario) {
		return new UsuarioRespuesta(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getRol(),
				usuario.isActivo(),
				nombres(usuario));
	}

	public static EstilistaRespuesta aEstilista(Usuario usuario) {
		return new EstilistaRespuesta(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.isActivo(),
				nombres(usuario));
	}

	private static List<String> nombres(Usuario usuario) {
		if (usuario.getServicios() == null) {
			return List.of();
		}
		List<Servicio> servicios = new ArrayList<>(usuario.getServicios());
		servicios.sort(Comparator.comparing(Servicio::getId, Comparator.nullsLast(Long::compareTo)));
		return servicios.stream().map(Servicio::getNombre).toList();
	}

}
