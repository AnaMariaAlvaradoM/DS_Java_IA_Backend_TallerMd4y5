package com.devsenior.sillon.solicitud;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.solicitud.dto.SolicitudContactoResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudContactoController {

    private final SolicitudContactoService servicio;

    public SolicitudContactoController(SolicitudContactoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<List<SolicitudContactoResponseDto>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicio.listarPorSalon(principal.getSalonId()));
    }

    @PatchMapping("/{id}/atendida")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<SolicitudContactoResponseDto> marcarAtendida(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicio.marcarAtendida(id, principal.getSalonId()));
    }
}
