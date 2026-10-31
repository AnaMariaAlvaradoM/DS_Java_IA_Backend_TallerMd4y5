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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "3 Servicios")
@SecurityRequirement(name = "bearerAuth")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(principal.getSalonId(), request));
    }

    @PutMapping("/{id}")
    public ServicioRespuesta actualizar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        return servicioService.actualizar(principal.getSalonId(), id, request);
    }

    @GetMapping
    public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return servicioService.listarPorSalon(principal.getSalonId());
    }
}
