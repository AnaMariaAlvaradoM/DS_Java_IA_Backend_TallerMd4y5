package com.devsenior.sillon.salon;

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

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public SalonRespuesta obtener(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return salonService.obtenerPorSalonId(principal.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonRespuesta actualizarConfiguracion(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConfiguracionSalonRequest request) {
        return salonService.actualizarConfiguracion(principal.getSalonId(), request);
    }
}
