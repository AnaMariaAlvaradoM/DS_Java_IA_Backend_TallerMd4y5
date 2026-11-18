package com.devsenior.sillon.servicio;

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
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;

import jakarta.validation.Valid;

/**
 * Servicios · /api/servicios (SPEC.md §7.7). POST y PUT son del DUENO;
 * el GET también lo usa el ESTILISTA (reglas en ConfiguracionSeguridad).
 */
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ServicioRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioService.crear(principal.getSalonId(), peticion));
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicioService.listar(principal.getSalonId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest peticion) {
        return ResponseEntity.ok(servicioService.actualizar(principal.getSalonId(), id, peticion));
    }
}
