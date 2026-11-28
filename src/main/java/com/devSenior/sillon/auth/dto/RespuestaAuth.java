package com.devSenior.sillon.auth.dto;

import com.devSenior.sillon.usuario.entidad.Rol;

public record RespuestaAuth(
		String token,
		String tipo,
		int expiraEnMinutos,
		Long usuarioId,
		String nombre,
		Rol rol,
		Long salonId,
		String salonSlug) {
}
