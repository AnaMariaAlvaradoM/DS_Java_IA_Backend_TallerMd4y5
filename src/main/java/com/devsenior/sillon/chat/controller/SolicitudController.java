package com.devsenior.sillon.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.chat.dto.SolicitudResponse;
import com.devsenior.sillon.chat.service.SolicitudService;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solicitudes que dejó el chatbot, para que el dueño las atienda. */
@Tag(name = "8 Solicitudes", description = "Solicitudes de contacto que dejó el chatbot")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<SolicitudResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return solicitudService.listar(usuario.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudResponse marcarAtendida(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return solicitudService.marcarAtendida(usuario.getSalonId(), id);
    }
}
