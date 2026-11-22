package com.devsenior.sillon.salon;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

/** Mi salon (solo DUENO, regla en ConfiguracionSeguridad). El salonId sale del usuario autenticado. */
@RestController
@RequestMapping("/api/salon")
public class SalonControlador {

    private final SalonServicio salonServicio;

    public SalonControlador(SalonServicio salonServicio) {
        this.salonServicio = salonServicio;
    }

    @GetMapping
    public SalonRespuesta obtener(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return salonServicio.obtener(usuario.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonRespuesta actualizarConfiguracion(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiguracionPeticion peticion) {
        return salonServicio.actualizarConfiguracion(usuario.getSalonId(), peticion);
    }
}
