package com.devSenior.sillon.usuario.dto;

import java.util.List;

import com.devSenior.sillon.usuario.entidad.Rol;

public record UsuarioRespuesta(
		Long id,
		String nombre,
		String email,
		Rol rol,
		boolean activo,
		List<String> servicios) {
}
