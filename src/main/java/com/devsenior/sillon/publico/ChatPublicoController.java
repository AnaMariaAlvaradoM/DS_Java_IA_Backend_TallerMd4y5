
package com.devsenior.sillon.publico;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.ia.RespuestaChatIA;
import com.devsenior.sillon.ia.ServicioChatIA;
import com.devsenior.sillon.ia.SolicitudChatIA;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
@Tag(
    name = "Chatbot público",
    description = "Asistente virtual con inteligencia artificial para consultas de clientes y escalamiento a atención humana"
)
public class ChatPublicoController {

    private final ServicioChatIA servicioChatIA;

    public ChatPublicoController(
            ServicioChatIA servicioChatIA
    ) {
        this.servicioChatIA = servicioChatIA;
    }

    @PostMapping("/{slug}/chat")
    public RespuestaChatIA responder(
            @PathVariable String slug,
            @Valid @RequestBody SolicitudChatIA solicitud
    ) {
        return servicioChatIA.responder(
                slug,
                solicitud
        );
    }
}
