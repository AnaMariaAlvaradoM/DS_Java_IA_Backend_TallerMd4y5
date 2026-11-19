package com.devsenior.sillon.salon.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.salon.dto.ConfiguracionRequest;
import com.devsenior.sillon.salon.dto.SalonResponse;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2 Mi salón", description = "Datos, plan, créditos y horario del salón del dueño")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public SalonResponse miSalon(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return salonService.obtener(usuario.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonResponse configurar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                    @Valid @RequestBody ConfiguracionRequest request) {
        return salonService.actualizarConfiguracion(usuario.getSalonId(), request);
    }
}
