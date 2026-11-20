package com.devsenior.sillon.chat;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping("/{slug}/chat")
    public ChatResponse responder(@PathVariable String slug, @Valid @RequestBody ChatRequest peticion) {
        return chat.responder(slug, peticion);
    }
}
