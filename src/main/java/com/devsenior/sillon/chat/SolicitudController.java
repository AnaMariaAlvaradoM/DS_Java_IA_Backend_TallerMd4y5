package com.devsenior.sillon.chat;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudService solicitudes;
    public SolicitudController(SolicitudService solicitudes) { this.solicitudes = solicitudes; }
    @GetMapping
    public List<SolicitudResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return solicitudes.listar(principal.getSalonId());
    }
    @PatchMapping("/{id}/atendida")
    public SolicitudResponse atender(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return solicitudes.atender(principal.getSalonId(), id);
    }
}
