package com.devsenior.sillon.salon;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.salon.dto.ConfiguracionRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public ResponseEntity<SalonResponse> obtenerMiSalon(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(salonService.obtenerMiSalon(principal.getSalonId()));
    }

    @PutMapping("/configuracion")
    public ResponseEntity<SalonResponse> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConfiguracionRequest peticion) {
        return ResponseEntity.ok(salonService.actualizarConfiguracion(principal.getSalonId(), peticion));
    }
}
