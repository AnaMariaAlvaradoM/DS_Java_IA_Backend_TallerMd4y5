package com.devSenior.sillon.cliente.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.cliente.dto.ClienteRespuesta;
import com.devSenior.sillon.cliente.dto.SolicitudCliente;
import com.devSenior.sillon.cliente.servicio.ClienteServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteControlador {

	private final ClienteServicio clienteServicio;

	public ClienteControlador(ClienteServicio clienteServicio) {
		this.clienteServicio = clienteServicio;
	}

	@PostMapping
	@Operation(summary = "Crea un cliente del salón autenticado. Rol DUENO.")
	@ApiResponse(responseCode = "201", description = "Cliente creado")
	@ApiResponse(responseCode = "409", description = "Teléfono repetido en el salón")
	public ResponseEntity<ClienteRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody SolicitudCliente solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clienteServicio.crear(principal.getSalonId(), solicitud));
	}

	@GetMapping
	@Operation(summary = "Lista o busca clientes del salón por nombre o teléfono. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Clientes del salón")
	public List<ClienteRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@RequestParam(required = false) String q) {
		return clienteServicio.listar(principal.getSalonId(), q);
	}

}
