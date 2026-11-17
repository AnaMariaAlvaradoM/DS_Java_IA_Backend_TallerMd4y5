package com.devSenior.sillon.cita.controlador;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.cita.dto.CitaRespuesta;
import com.devSenior.sillon.cita.dto.SolicitudCita;
import com.devSenior.sillon.cita.dto.SolicitudEstado;
import com.devSenior.sillon.cita.entidad.EstadoCita;
import com.devSenior.sillon.cita.servicio.CitaServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "Citas")
@SecurityRequirement(name = "bearerAuth")
public class CitaControlador {

	private final CitaServicio citaServicio;

	public CitaControlador(CitaServicio citaServicio) {
		this.citaServicio = citaServicio;
	}

	@PostMapping
	@Operation(summary = "Agenda una cita del salón. Nace PENDIENTE con origen SALON. Rol DUENO.")
	@ApiResponse(responseCode = "201", description = "Cita creada")
	@ApiResponse(responseCode = "400", description = "Horario, domingo, intervalo, pasado o anticipación")
	@ApiResponse(responseCode = "404", description = "Servicio o estilista no pertenecen al salón")
	@ApiResponse(responseCode = "409", description = "Solapamiento o el estilista no ofrece el servicio")
	public ResponseEntity<CitaRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody SolicitudCita solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(citaServicio.crearPorSalon(principal.getSalonId(), solicitud));
	}

	@GetMapping
	@Operation(summary = "Lista citas del salón por inicio. El estilista solo ve las suyas.")
	@ApiResponse(responseCode = "200", description = "Citas ordenadas por inicio")
	public List<CitaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta,
			@RequestParam(required = false) EstadoCita estado,
			@RequestParam(required = false) Long estilistaId) {
		return citaServicio.listar(principal, desde, hasta, estado, estilistaId);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtiene una cita del salón autenticado.")
	@ApiResponse(responseCode = "200", description = "Cita")
	@ApiResponse(responseCode = "404", description = "No existe, es de otro salón o no es del estilista")
	public CitaRespuesta obtener(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
		return citaServicio.obtener(principal, id);
	}

	@PatchMapping("/{id}/estado")
	@Operation(summary = "Cambia el estado de una cita según la máquina de estados.")
	@ApiResponse(responseCode = "200", description = "Estado actualizado")
	@ApiResponse(responseCode = "403", description = "El estilista no puede usar ese destino")
	@ApiResponse(responseCode = "404", description = "No existe o no es del estilista")
	@ApiResponse(responseCode = "409", description = "Transición no permitida")
	public CitaRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
			@Valid @RequestBody SolicitudEstado solicitud) {
		return citaServicio.cambiarEstado(principal, id, solicitud.estado());
	}

}
