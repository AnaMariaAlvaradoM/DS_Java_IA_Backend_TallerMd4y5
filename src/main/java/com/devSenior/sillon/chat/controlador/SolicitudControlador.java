package com.devSenior.sillon.chat.controlador;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.chat.dto.SolicitudRespuesta;
import com.devSenior.sillon.chat.entidad.EstadoSolicitud;
import com.devSenior.sillon.chat.servicio.SolicitudContactoServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes")
@SecurityRequirement(name = "bearerAuth")
public class SolicitudControlador {

	private final SolicitudContactoServicio solicitudContactoServicio;

	public SolicitudControlador(SolicitudContactoServicio solicitudContactoServicio) {
		this.solicitudContactoServicio = solicitudContactoServicio;
	}

	@GetMapping
	@Operation(summary = "Lista las solicitudes del salón, de la más reciente a la más antigua. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Solicitudes del salón")
	public List<SolicitudRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@RequestParam(required = false) EstadoSolicitud estado) {
		return solicitudContactoServicio.listar(principal.getSalonId(), estado);
	}

	@PatchMapping("/{id}/atendida")
	@Operation(summary = "Marca una solicitud como atendida. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Solicitud atendida")
	@ApiResponse(responseCode = "404", description = "No existe o es de otro salón")
	@ApiResponse(responseCode = "409", description = "Ya estaba atendida")
	public SolicitudRespuesta atender(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
		return solicitudContactoServicio.atender(principal.getSalonId(), id);
	}

}
