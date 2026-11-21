package com.devsenior.sillon.salon;

import com.devsenior.sillon.salon.dto.ActualizarConfiguracionRequest;
import com.devsenior.sillon.salon.dto.SalonRespuesta;
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
    public ResponseEntity<SalonRespuesta> obtenerMiSalon(@AuthenticationPrincipal UsuarioPrincipal principal) {
        SalonRespuesta respuesta = salonService.obtenerMiSalon(principal.getSalonId());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/configuracion")
    public ResponseEntity<SalonRespuesta> actualizarConfiguracion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ActualizarConfiguracionRequest request
    ) {
        SalonRespuesta respuesta = salonService.actualizarConfiguracion(principal.getSalonId(), request);
        return ResponseEntity.ok(respuesta);
    }
}
