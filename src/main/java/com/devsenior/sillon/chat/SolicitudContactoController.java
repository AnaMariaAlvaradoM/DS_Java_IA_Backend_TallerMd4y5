package com.devsenior.sillon.chat;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.chat.dto.SolicitudResponse;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudContactoController {

    private final SolicitudContactoService solicitudes;

    public SolicitudContactoController(SolicitudContactoService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping
    public List<SolicitudResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return solicitudes.listar(principal.getSalonId());
    }

    @PatchMapping("/{id}/atendida")
    public SolicitudResponse atender(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return solicitudes.atender(principal.getSalonId(), id);
    }
}
