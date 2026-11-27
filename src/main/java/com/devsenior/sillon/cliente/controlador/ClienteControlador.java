package com.devsenior.sillon.cliente.controlador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cliente.dto.RespuestaCliente;
import com.devsenior.sillon.cliente.servicio.ClienteServicio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "5 Clientes", description = "Consulta de clientes del salón")
@SecurityRequirement(name = "Bearer JWT")
public class ClienteControlador {

	private final ClienteServicio clienteServicio;

	public ClienteControlador(ClienteServicio clienteServicio) {
		this.clienteServicio = clienteServicio;
	}

	@GetMapping
	public ResponseEntity<List<RespuestaCliente>> listar(
			@AuthenticationPrincipal UsuarioPrincipal usuario) {

		return ResponseEntity.ok(
				clienteServicio.listar(usuario.getSalonId()));
	}

}
