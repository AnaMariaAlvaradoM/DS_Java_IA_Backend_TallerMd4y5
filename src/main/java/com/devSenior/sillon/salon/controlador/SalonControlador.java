package com.devSenior.sillon.salon.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devSenior.sillon.salon.dto.SalonRespuesta;
import com.devSenior.sillon.salon.dto.SolicitudConfiguracion;
import com.devSenior.sillon.salon.servicio.SalonServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salon")
public class SalonControlador {

	private final SalonServicio salonServicio;

	public SalonControlador(SalonServicio salonServicio) {
		this.salonServicio = salonServicio;
	}

	/**
	 * Obtiene la información del salón del usuario autenticado.
	 */
	@GetMapping
	public SalonRespuesta obtener(
			@AuthenticationPrincipal UsuarioPrincipal principal) {

		Long salonId = obtenerSalonId(principal);

		return salonServicio.obtener(salonId);
	}

	/**
	 * Actualiza la configuración del salón del usuario autenticado.
	 */
	@PutMapping("/configuracion")
	public SalonRespuesta actualizarConfiguracion(
			@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody SolicitudConfiguracion solicitud) {

		Long salonId = obtenerSalonId(principal);

		return salonServicio.actualizarConfiguracion(
				salonId,
				solicitud
		);
	}

	/**
	 * Comprueba que el usuario esté autenticado y tenga un salón asociado.
	 */
	private Long obtenerSalonId(UsuarioPrincipal principal) {

		if (principal == null) {
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED,
					"Usuario no autenticado"
			);
		}

		if (principal.getSalonId() == null) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"El usuario no tiene un salón asociado"
			);
		}

		return principal.getSalonId();
	}
}