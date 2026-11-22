package com.devSenior.sillon.servicio.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.seguridad.UsuarioPrincipal;
import com.devSenior.sillon.servicio.dto.ServicioRespuesta;
import com.devSenior.sillon.servicio.dto.SolicitudServicio;
import com.devSenior.sillon.servicio.servicio.ServicioServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "3. Servicios")
@SecurityRequirement(name = "bearerAuth")
public class ServicioControlador {

	private final ServicioServicio servicioServicio;

	public ServicioControlador(ServicioServicio servicioServicio) {
		this.servicioServicio = servicioServicio;
	}

	@PostMapping
	@Operation(summary = "Crea un servicio del salón autenticado. Rol DUENO.")
	@ApiResponse(responseCode = "201", description = "Servicio creado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos")
	public ResponseEntity<ServicioRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody SolicitudServicio solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(servicioServicio.crear(principal.getSalonId(), solicitud));
	}

	@GetMapping
	@Operation(summary = "Lista los servicios del salón. Roles DUENO y ESTILISTA. Filtro opcional activo.")
	@ApiResponse(responseCode = "200", description = "Lista del salón")
	public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@RequestParam(required = false) Boolean activo) {
		return servicioServicio.listar(principal.getSalonId(), activo);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualiza un servicio del salón. Rol DUENO. No elimina: activo=false desactiva.")
	@ApiResponse(responseCode = "200", description = "Servicio actualizado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos")
	@ApiResponse(responseCode = "404", description = "No existe o es de otro salón")
	public ServicioRespuesta actualizar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
			@Valid @RequestBody SolicitudServicio solicitud) {
		return servicioServicio.actualizar(principal.getSalonId(), id, solicitud);
	}

}
