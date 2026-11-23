package com.devsenior.sillon.chat;

import java.util.List;

import org.springframework.http.ResponseEntity;
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

    private final SolicitudServicio solicitudServicio;

    public SolicitudController(SolicitudServicio solicitudServicio) {
        this.solicitudServicio = solicitudServicio;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudRespuesta>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudServicio.listar(principal.getSalonId()));
    }

    @PatchMapping("/{id}/atendida")
    public ResponseEntity<SolicitudRespuesta> atendida(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(solicitudServicio.atender(principal.getSalonId(), id));
    }
}
