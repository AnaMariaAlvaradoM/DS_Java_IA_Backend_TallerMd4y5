package com.devsenior.sillon.chatbot.contacto.controller;

import com.devsenior.sillon.chatbot.contacto.dto.SolicitudContactoResponse;
import com.devsenior.sillon.chatbot.contacto.service.SolicitudContactoService;
import com.devsenior.sillon.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@PreAuthorize("hasRole('DUENO')")
public class SolicitudContactoController {

    private final SolicitudContactoService solicitudContactoService;

    public SolicitudContactoController(SolicitudContactoService solicitudContactoService) {
        this.solicitudContactoService = solicitudContactoService;
    }

    // GET /api/solicitudes — 200 con la lista (más recientes primero)
    @GetMapping
    public ResponseEntity<List<SolicitudContactoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<SolicitudContactoResponse> respuestas = solicitudContactoService.obtenerSolicitudes(usuario.getSalonId());
        return ResponseEntity.ok(respuestas);
    }

    // PATCH /api/solicitudes/{id}/atendida — 200, 404 o 409
    @PatchMapping("/{id}/atendida")
    public ResponseEntity<Void> marcarComoAtendida(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        solicitudContactoService.marcarComoAtendida(id, usuario.getSalonId());

        // Retorna HTTP 200 OK sin cuerpo (ya que es un PATCH)
        return ResponseEntity.ok().build();
    }
}