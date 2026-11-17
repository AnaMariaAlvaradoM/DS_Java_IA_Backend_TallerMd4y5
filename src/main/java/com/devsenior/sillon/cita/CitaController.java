package com.devsenior.sillon.cita;

import java.time.LocalDate;
import java.time.Clock;
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

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaServicio citaServicio;
    private final Clock reloj;

    public CitaController(CitaServicio citaServicio, Clock reloj) {
        this.citaServicio = citaServicio;
        this.reloj = reloj;
    }

    @PostMapping
    public ResponseEntity<CitaRespuesta> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CitaSolicitud solicitud) {
        CitaRespuesta respuesta = citaServicio.crearDelSalon(principal.getSalonId(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<CitaRespuesta>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        return ResponseEntity.ok(citaServicio.listar(principal.getSalonId(), dia, principal));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaRespuesta> cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EstadoSolicitud solicitud) {
        return ResponseEntity.ok(citaServicio.cambiarEstado(principal.getSalonId(), principal, id,
                solicitud.estado()));
    }
}
