package com.devsenior.sillon.usuario.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.dto.RespuestaEstilista;
import com.devsenior.sillon.usuario.dto.SolicitudEstilista;
import com.devsenior.sillon.usuario.servicio.EstilistaServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estilistas")
public class EstilistaControlador {

	private final EstilistaServicio estilistaServicio;

	public EstilistaControlador(EstilistaServicio estilistaServicio) {
		this.estilistaServicio = estilistaServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaEstilista> registrar(
			@AuthenticationPrincipal UsuarioPrincipal usuario,
			@Valid @RequestBody SolicitudEstilista solicitud) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(estilistaServicio.registrar(usuario.getSalonId(), solicitud));
	}

	@GetMapping
	public ResponseEntity<List<RespuestaEstilista>> listar(
			@AuthenticationPrincipal UsuarioPrincipal usuario) {

		return ResponseEntity.ok(
				estilistaServicio.listar(usuario.getSalonId()));
	}

}
