package com.devsenior.sillon.servicio.controlador;

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
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.RespuestaServicio;
import com.devsenior.sillon.servicio.dto.SolicitudServicio;
import com.devsenior.sillon.servicio.servicio.ServicioServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios")
public class ServicioControlador {

	private final ServicioServicio servicioServicio;

	public ServicioControlador(ServicioServicio servicioServicio) {
		this.servicioServicio = servicioServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaServicio> crear(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@Valid @RequestBody SolicitudServicio solicitud) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(servicioServicio.crear(usuario.getSalonId(), solicitud));
	}

	@GetMapping
	public ResponseEntity<List<RespuestaServicio>> listar(
			@AuthenticationPrincipal UsuarioPrincipal usuario) {

		return ResponseEntity.ok(
				servicioServicio.listar(usuario.getSalonId()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RespuestaServicio> actualizar(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@PathVariable Long id,
			@Valid @RequestBody SolicitudServicio solicitud) {

		return ResponseEntity.ok(
				servicioServicio.actualizar(usuario.getSalonId(), id, solicitud));
	}

}
