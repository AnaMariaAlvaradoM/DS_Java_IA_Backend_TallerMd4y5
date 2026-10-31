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

    private final SolicitudContactoService solicitudContactoService;

    public SolicitudController(SolicitudContactoService solicitudContactoService) {
        this.solicitudContactoService = solicitudContactoService;
    }

    @GetMapping
    public List<SolicitudRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return solicitudContactoService.listarPorSalon(principal.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudRespuesta marcarAtendida(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return solicitudContactoService.marcarAtendida(principal.getSalonId(), id);
    }
}
