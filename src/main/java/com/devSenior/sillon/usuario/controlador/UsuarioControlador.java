package com.devSenior.sillon.usuario.controlador;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.seguridad.UsuarioPrincipal;
import com.devSenior.sillon.usuario.dto.SolicitudActivo;
import com.devSenior.sillon.usuario.dto.SolicitudServiciosUsuario;
import com.devSenior.sillon.usuario.dto.UsuarioRespuesta;
import com.devSenior.sillon.usuario.servicio.EstilistaServicio;
import com.devSenior.sillon.usuario.servicio.UsuarioServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioControlador {

	private final UsuarioServicio usuarioServicio;
	private final EstilistaServicio estilistaServicio;

	public UsuarioControlador(UsuarioServicio usuarioServicio, EstilistaServicio estilistaServicio) {
		this.usuarioServicio = usuarioServicio;
		this.estilistaServicio = estilistaServicio;
	}

	@GetMapping("/yo")
	@Operation(summary = "Datos del usuario autenticado. Cualquier rol. No recibe identificadores externos.")
	@ApiResponse(responseCode = "200", description = "Usuario autenticado, sin contraseña")
	public UsuarioRespuesta yo(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return usuarioServicio.yo(principal.getId());
	}

	@GetMapping
	@Operation(summary = "Lista los usuarios del salón autenticado. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Usuarios del salón")
	@ApiResponse(responseCode = "403", description = "El rol no es DUENO")
	public List<UsuarioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return usuarioServicio.listar(principal.getSalonId());
	}

	@PatchMapping("/{id}/activo")
	@Operation(summary = "Activa o desactiva un usuario del salón. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Estado actualizado")
	@ApiResponse(responseCode = "404", description = "No existe o es de otro salón")
	public UsuarioRespuesta cambiarActivo(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
			@Valid @RequestBody SolicitudActivo solicitud) {
		return usuarioServicio.cambiarActivo(principal.getSalonId(), id, solicitud);
	}

	@PutMapping("/{id}/servicios")
	@Operation(summary = "Reemplaza los servicios de un usuario del salón. Rol DUENO. Solo servicios activos del mismo salón.")
	@ApiResponse(responseCode = "200", description = "Asignación reemplazada")
	@ApiResponse(responseCode = "400", description = "Servicio inactivo")
	@ApiResponse(responseCode = "404", description = "Usuario o servicio inexistente o de otro salón")
	public UsuarioRespuesta asignarServicios(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long id, @Valid @RequestBody SolicitudServiciosUsuario solicitud) {
		return estilistaServicio.asignarServicios(principal.getSalonId(), id, solicitud.servicioIds());
	}

}
