package com.devSenior.sillon.usuario.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.seguridad.UsuarioPrincipal;
import com.devSenior.sillon.usuario.dto.EstilistaRespuesta;
import com.devSenior.sillon.usuario.dto.SolicitudEstilista;
import com.devSenior.sillon.usuario.servicio.EstilistaServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estilistas")
@Tag(name = "4. Estilistas")
@SecurityRequirement(name = "bearerAuth")
public class EstilistaControlador {

	private final EstilistaServicio estilistaServicio;

	public EstilistaControlador(EstilistaServicio estilistaServicio) {
		this.estilistaServicio = estilistaServicio;
	}

	@PostMapping
	@Operation(summary = "Crea un estilista. Rol DUENO. Orden: límite del plan 402, correo 409, servicios del salón 404.")
	@ApiResponse(responseCode = "201", description = "Estilista creado, sin contraseña")
	@ApiResponse(responseCode = "400", description = "Datos inválidos o servicio inactivo")
	@ApiResponse(responseCode = "402", description = "Límite del plan GRATIS")
	@ApiResponse(responseCode = "404", description = "Servicio inexistente o de otro salón")
	@ApiResponse(responseCode = "409", description = "Correo repetido")
	public ResponseEntity<EstilistaRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody SolicitudEstilista solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(estilistaServicio.crear(principal.getSalonId(), solicitud));
	}

	@GetMapping
	@Operation(summary = "Lista los estilistas del salón autenticado. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Estilistas del salón")
	public List<EstilistaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return estilistaServicio.listar(principal.getSalonId());
	}

}
