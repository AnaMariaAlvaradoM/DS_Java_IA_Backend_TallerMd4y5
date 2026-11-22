package com.devSenior.sillon.chat.controlador;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.chat.dto.ChatRespuesta;
import com.devSenior.sillon.chat.dto.SolicitudChat;
import com.devSenior.sillon.chat.servicio.ChatServicio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones/{slug}/chat")
@Tag(name = "0. Público")
@SecurityRequirements
public class ChatControlador {

	private final ChatServicio chatServicio;

	public ChatControlador(ChatServicio chatServicio) {
		this.chatServicio = chatServicio;
	}

	@PostMapping
	@Operation(summary = "Chat público del salón. La IA redacta; Java verifica catálogo, créditos y promesas.")
	@ApiResponse(responseCode = "200", description = "Respuesta del asistente")
	@ApiResponse(responseCode = "400", description = "Historial vacío, demasiado largo o turno inválido")
	@ApiResponse(responseCode = "402", description = "Sin créditos")
	@ApiResponse(responseCode = "404", description = "El salón no existe")
	@ApiResponse(responseCode = "502", description = "JSON de la IA inválido")
	@ApiResponse(responseCode = "503", description = "Ningún proveedor disponible")
	public ChatRespuesta chat(@PathVariable String slug, @Valid @RequestBody SolicitudChat solicitud) {
		return chatServicio.responder(slug, solicitud);
	}

}
