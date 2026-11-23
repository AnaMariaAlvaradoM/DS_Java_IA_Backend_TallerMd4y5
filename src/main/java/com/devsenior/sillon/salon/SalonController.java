package com.devsenior.sillon.salon;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonServicio salonServicio;

    public SalonController(SalonServicio salonServicio) {
        this.salonServicio = salonServicio;
    }

    @GetMapping
    public ResponseEntity<SalonRespuesta> obtener(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(salonServicio.obtener(principal.getSalonId()));
    }

    @PutMapping("/configuracion")
    public ResponseEntity<SalonRespuesta> configurar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConfiguracionSolicitud solicitud) {
        return ResponseEntity.ok(salonServicio.configurar(principal.getSalonId(), solicitud));
    }
}
