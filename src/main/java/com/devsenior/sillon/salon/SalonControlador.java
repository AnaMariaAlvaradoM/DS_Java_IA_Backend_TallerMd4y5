package com.devsenior.sillon.salon;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salon")
public class SalonControlador {
    private final SalonServicio servicio;

    public SalonControlador(SalonServicio servicio) { this.servicio = servicio; }

    @GetMapping
    public SalonRespuesta consultar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.consultar(usuario.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonRespuesta configurar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ConfiguracionSolicitud solicitud) {
        return servicio.configurar(usuario.getSalonId(), solicitud);
    }
}
