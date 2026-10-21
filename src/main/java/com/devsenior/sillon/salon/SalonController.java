package com.devsenior.sillon.salon;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2 Mi salón")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/salon")
public class SalonController {
    private final SalonService salones;

    public SalonController(SalonService salones) { this.salones = salones; }

    @GetMapping
    public SalonResponse consultar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return salones.consultar(principal.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonResponse configurar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConfiguracionSalonRequest request) {
        return salones.configurar(principal.getSalonId(), request);
    }
}
