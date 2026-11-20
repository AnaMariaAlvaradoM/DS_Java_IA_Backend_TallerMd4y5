package com.devSenior.sillon.salon.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.salon.dto.SalonRespuesta;
import com.devSenior.sillon.salon.servicio.SalonServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración")
@SecurityRequirement(name = "bearerAuth")
public class AdminControlador {

	private final SalonServicio salonServicio;

	public AdminControlador(SalonServicio salonServicio) {
		this.salonServicio = salonServicio;
	}

	@GetMapping("/salones")
	@Operation(summary = "Lista todos los salones de la plataforma. Rol ADMIN.")
	@ApiResponse(responseCode = "200", description = "Salones de la plataforma")
	@ApiResponse(responseCode = "401", description = "Sin token")
	@ApiResponse(responseCode = "403", description = "El rol no es ADMIN")
	public List<SalonRespuesta> listarSalones() {
		return salonServicio.listarTodos();
	}

}
