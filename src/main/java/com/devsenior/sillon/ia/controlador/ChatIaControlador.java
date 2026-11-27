
package com.devsenior.sillon.ia.controlador;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.devsenior.sillon.ia.dto.RespuestaChatIa;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.servicio.ChatIaServicio;

import io.swagger.v3.oas.annotations.tags.Tag;


@Validated
@RestController
@RequestMapping("/api/publico/salones")
@Tag(name = "0 Público", description = "Chatbot público de atención al cliente")
public class ChatIaControlador {


    private final ChatIaServicio chatIaServicio;

    public ChatIaControlador(
            ChatIaServicio chatIaServicio) {

        this.chatIaServicio = chatIaServicio;
    }

    @PostMapping("/{slug}/chat")
    public ResponseEntity<RespuestaChatIa> conversar(
            @PathVariable String slug,
            @Valid @RequestBody SolicitudChatIa solicitud) {

        RespuestaChatIa respuesta = chatIaServicio.responder(slug, solicitud);

        return ResponseEntity.ok(respuesta);
    }
}
