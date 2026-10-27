package com.devsenior.sillon.chat;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "0 Público")
@RestController
@RequestMapping("/api/publico/salones/{slug}/chat")
public class ChatController {
    private final ChatService chat;
    public ChatController(ChatService chat) { this.chat = chat; }
    @PostMapping
    public ChatResponse responder(@PathVariable String slug, @Valid @RequestBody ChatRequest request) {
        return chat.responder(slug, request);
    }
}
