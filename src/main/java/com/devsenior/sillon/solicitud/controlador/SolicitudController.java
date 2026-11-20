package com.devsenior.sillon.solicitud.controlador;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.solicitud.dto.SolicitudResponse;
import com.devsenior.sillon.solicitud.servicio.SolicitudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(solicitudService.listarPorSalon(usuario.getSalonId()));
    }

    @PatchMapping("/{id}/atendida")
    public ResponseEntity<SolicitudResponse> marcarAtendida(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.marcarAtendida(usuario.getSalonId(), id));
    }
}