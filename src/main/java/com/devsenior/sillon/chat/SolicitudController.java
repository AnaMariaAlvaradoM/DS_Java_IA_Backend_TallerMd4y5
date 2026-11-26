package com.devsenior.sillon.chat;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<SolicitudRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return solicitudService.listar(usuario.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudRespuesta atendida(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return solicitudService.marcarAtendida(usuario.getSalonId(), id);
    }
}
