package com.devsenior.sillon.chat;

import com.devsenior.sillon.chat.dto.SolicitudContactoRespuesta;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudContactoController {

    private final ChatService chatService;

    public SolicitudContactoController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudContactoRespuesta>> listarSolicitudes(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        List<SolicitudContactoRespuesta> solicitudes = chatService.listarSolicitudes(principal.getSalonId());
        return ResponseEntity.ok(solicitudes);
    }

    @PatchMapping("/{id}/atendida")
    public ResponseEntity<SolicitudContactoRespuesta> marcarComoAtendida(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id
    ) {
        SolicitudContactoRespuesta respuesta = chatService.marcarComoAtendida(principal.getSalonId(), id);
        return ResponseEntity.ok(respuesta);
    }
}
