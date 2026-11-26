package com.devsenior.sillon.chat;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Chat público del salón (sin token). */
@RestController
public class ChatController {

    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/api/publico/salones/{slug}/chat")
    public ChatRespuesta chat(@PathVariable String slug, @Valid @RequestBody ChatPeticion peticion) {
        return chatbotService.responder(slug, peticion);
    }
}
