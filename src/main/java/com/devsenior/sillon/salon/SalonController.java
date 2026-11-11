package com.devsenior.sillon.salon;

import com.devsenior.sillon.salon.dto.ActualizarConfiguracionDto;
import com.devsenior.sillon.salon.dto.SalonResponseDto;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public ResponseEntity<SalonResponseDto> obtenerMiSalon(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        SalonResponseDto response = salonService.obtenerMiSalon(usuario.getSalonId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<SalonResponseDto> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ActualizarConfiguracionDto request) {

        SalonResponseDto response = salonService.actualizarConfiguracion(usuario.getSalonId(), request);
        return ResponseEntity.ok(response);
    }
}