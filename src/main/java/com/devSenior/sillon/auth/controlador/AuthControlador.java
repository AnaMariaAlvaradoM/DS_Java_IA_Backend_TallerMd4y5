package com.devSenior.sillon.auth.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.auth.dto.RespuestaAuth;
import com.devSenior.sillon.auth.dto.SolicitudLogin;
import com.devSenior.sillon.auth.dto.SolicitudRegistro;
import com.devSenior.sillon.auth.servicio.AuthServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticación")
@SecurityRequirements
public class AuthControlador {

	private final AuthServicio authServicio;

	public AuthControlador(AuthServicio authServicio) {
		this.authServicio = authServicio;
	}

	@PostMapping("/registro")
	@Operation(summary = "Registra un salón y su dueño. Devuelve un token JWT.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Salón y dueño registrados correctamente"),
			@ApiResponse(responseCode = "400", description = "Datos inválidos"),
			@ApiResponse(responseCode = "409", description = "Correo o slug duplicado")
	})
	public ResponseEntity<RespuestaAuth> registrar(
			@Valid @RequestBody SolicitudRegistro solicitud) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(authServicio.registrar(solicitud));
	}

	@PostMapping("/login")
	@Operation(summary = "Autentica un usuario y devuelve un token JWT.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Autenticación correcta"),
			@ApiResponse(responseCode = "400", description = "Datos inválidos"),
			@ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
	})
	public RespuestaAuth login(
			@Valid @RequestBody SolicitudLogin solicitud) {

		return authServicio.login(solicitud);
	}
}