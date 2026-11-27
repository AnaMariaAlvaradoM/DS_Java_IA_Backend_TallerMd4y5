package com.devsenior.sillon.cita.controlador;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCambiarEstadoCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.servicio.CitaServicio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@RestController
@RequestMapping("/api/citas")
@Tag(name = "6 Agenda", description = "Gestión de citas, estados y agenda del salón")
@SecurityRequirement(name = "Bearer JWT")
public class CitaControlador {


	private final CitaServicio citaServicio;

	public CitaControlador(CitaServicio citaServicio) {
		this.citaServicio = citaServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaCita> crear(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@Valid @RequestBody SolicitudCrearCita solicitud) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(citaServicio.crear(usuario.getSalonId(), solicitud));
	}

	@GetMapping
	public ResponseEntity<List<RespuestaCita>> listar(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

		return ResponseEntity.ok(
				citaServicio.listarPorFecha(
						usuario.getSalonId(),
						usuario.getUsuarioId(),
						usuario.getRol(),
						fecha));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<RespuestaCita> cambiarEstado(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@PathVariable Long id,
			@Valid @RequestBody SolicitudCambiarEstadoCita solicitud) {

		return ResponseEntity.ok(
				citaServicio.cambiarEstado(
						usuario.getSalonId(),
						usuario.getUsuarioId(),
						usuario.getRol(),
						id,
						solicitud));
	}

}
