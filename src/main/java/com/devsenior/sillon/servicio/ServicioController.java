package com.devsenior.sillon.servicio;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.ServicioRequestDto;
import com.devsenior.sillon.servicio.dto.ServicioResponseDto;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ServicioResponseDto> crearServicio(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioRequestDto request) {

        ServicioResponseDto response = servicioService.crearServicio(usuario.getSalonId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDto>> listarServicios(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<ServicioResponseDto> lista = servicioService.listarServicios(usuario.getSalonId());
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ServicioResponseDto> actualizarServicio(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioRequestDto request) {

        ServicioResponseDto response = servicioService.actualizarServicio(id, usuario.getSalonId(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<Void> desactivarServicio(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        servicioService.cambiarEstado(id, usuario.getSalonId(), false);
        return ResponseEntity.noContent().build();
    }
}