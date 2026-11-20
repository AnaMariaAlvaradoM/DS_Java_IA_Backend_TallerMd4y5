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
public class SalonController {
    private final SalonService servicio;

    public SalonController(SalonService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public SalonResponse obtener(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.obtener(usuario.getSalonId());
    }

    @PutMapping("/configuracion")
    public SalonResponse configurar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                    @Valid @RequestBody ConfiguracionRequest request) {
        return servicio.configurar(usuario.getSalonId(), request);
    }
}
