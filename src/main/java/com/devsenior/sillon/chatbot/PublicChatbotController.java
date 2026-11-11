package com.devsenior.sillon.chatbot;

import com.devsenior.sillon.chatbot.dto.ChatPeticionDto;
import com.devsenior.sillon.chatbot.dto.ChatRespuestaDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/salones")
public class PublicChatbotController {

    private final ChatbotService chatbotService;

    public PublicChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/{slug}/chat")
    public ResponseEntity<ChatRespuestaDto> procesarChat(
            @PathVariable String slug,
            @Valid @RequestBody ChatPeticionDto peticion) {
        return ResponseEntity.ok(chatbotService.procesarChat(slug, peticion));
    }
}
