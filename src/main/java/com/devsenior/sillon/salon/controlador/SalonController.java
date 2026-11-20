package com.devsenior.sillon.salon.controlador;

import com.devsenior.sillon.salon.dto.ConfiguracionSalonRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.servicio.SalonService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public ResponseEntity<SalonResponse> obtenerMiSalon(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(salonService.obtenerMiSalon(usuario.getSalonId()));
    }

    @PutMapping("/configuracion")
    public ResponseEntity<SalonResponse> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiguracionSalonRequest request) {
        return ResponseEntity.ok(salonService.actualizarConfiguracion(usuario.getSalonId(), request));
    }
}