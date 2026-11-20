package com.devSenior.sillon.publico.controlador;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.cita.dto.CitaRespuesta;
import com.devSenior.sillon.cita.dto.SolicitudCita;
import com.devSenior.sillon.publico.dto.DisponibilidadRespuesta;
import com.devSenior.sillon.publico.dto.SalonPublico;
import com.devSenior.sillon.publico.servicio.PublicoServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
@Tag(name = "Público")
public class PublicoControlador {

	private final PublicoServicio publicoServicio;

	public PublicoControlador(PublicoServicio publicoServicio) {
		this.publicoServicio = publicoServicio;
	}

	@GetMapping("/{slug}")
	@Operation(summary = "Catálogo público del salón. Sin correos ni datos internos.")
	@ApiResponse(responseCode = "200", description = "Salón, servicios activos y estilistas activos")
	@ApiResponse(responseCode = "404", description = "Slug desconocido")
	public SalonPublico catalogo(@PathVariable String slug) {
		return publicoServicio.catalogo(slug);
	}

	@GetMapping("/{slug}/disponibilidad")
	@Operation(summary = "Horarios libres calculados en Java. No usa inteligencia artificial.")
	@ApiResponse(responseCode = "200", description = "Horarios libres")
	@ApiResponse(responseCode = "404", description = "Salón, servicio o estilista")
	@ApiResponse(responseCode = "409", description = "El estilista no ofrece el servicio")
	public DisponibilidadRespuesta disponibilidad(@PathVariable String slug, @RequestParam Long servicioId,
			@RequestParam Long estilistaId, @RequestParam LocalDate fecha) {
		return publicoServicio.disponibilidad(slug, servicioId, estilistaId, fecha);
	}

	@PostMapping("/{slug}/reservas")
	@Operation(summary = "Reserva pública. Nace PENDIENTE con origen EN_LINEA.")
	@ApiResponse(responseCode = "201", description = "Reserva creada")
	@ApiResponse(responseCode = "400", description = "Horario inválido")
	@ApiResponse(responseCode = "404", description = "Salón, servicio o estilista")
	@ApiResponse(responseCode = "409", description = "Solapamiento")
	public ResponseEntity<CitaRespuesta> reservar(@PathVariable String slug, @Valid @RequestBody SolicitudCita solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(publicoServicio.reservar(slug, solicitud));
	}

}
