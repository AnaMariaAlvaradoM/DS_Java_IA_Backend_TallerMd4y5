package com.devsenior.sillon.chat;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudService servicio;

    public SolicitudController(SolicitudService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<SolicitudRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal dueno) {
        return servicio.listar(dueno.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudRespuesta atender(@AuthenticationPrincipal UsuarioPrincipal dueno, @PathVariable Long id) {
        return servicio.atender(dueno.getSalonId(), id);
    }
}
