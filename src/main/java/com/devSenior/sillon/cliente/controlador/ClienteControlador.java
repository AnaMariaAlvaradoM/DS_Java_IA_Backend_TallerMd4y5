package com.devSenior.sillon.cliente.controlador;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.cliente.dto.ClienteRespuesta;
import com.devSenior.sillon.cliente.servicio.ClienteServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

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

	@GetMapping
	@Operation(summary = "Lista los clientes del salón. Solo lectura. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Clientes del salón")
	public List<ClienteRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return clienteServicio.listar(principal.getSalonId());
	}

}
