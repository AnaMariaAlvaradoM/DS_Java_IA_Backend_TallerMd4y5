package com.devsenior.sillon.salon;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

/** Mi salón: siempre el del usuario autenticado, nunca un id que llegue en la URL. */
@RestController
@RequestMapping("/api/salon")
public class SalonController {

    private final SalonService salonService;

    public SalonController(SalonService salonService) {
        this.salonService = salonService;
    }

    @GetMapping
    public SalonRespuesta obtener(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return salonService.obtener(usuario.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonRespuesta configurar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiguracionPeticion peticion) {
        return salonService.configurar(usuario.getSalonId(), peticion);
    }
}
