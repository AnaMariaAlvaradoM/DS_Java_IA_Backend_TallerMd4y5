package com.devsenior.sillon.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones/{slug}/chat")
public class ChatController {

    private final ChatServicio chatServicio;

    public ChatController(ChatServicio chatServicio) {
        this.chatServicio = chatServicio;
    }

    @PostMapping
    public ResponseEntity<ChatRespuesta> responder(@PathVariable String slug,
            @Valid @RequestBody ChatSolicitud solicitud) {
        return ResponseEntity.ok(chatServicio.responder(slug, solicitud));
    }
}
