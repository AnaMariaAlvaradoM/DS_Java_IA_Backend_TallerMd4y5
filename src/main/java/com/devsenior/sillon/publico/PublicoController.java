package com.devsenior.sillon.publico;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.chat.ChatRequest;
import com.devsenior.sillon.chat.ChatRespuesta;
import com.devsenior.sillon.chat.ChatbotService;
import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaRespuesta;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
@Tag(name = "0 Público")
public class PublicoController {

    private final CatalogoService catalogoService;
    private final ChatbotService chatbotService;

    public PublicoController(CatalogoService catalogoService, ChatbotService chatbotService) {
        this.catalogoService = catalogoService;
        this.chatbotService = chatbotService;
    }

    @GetMapping("/{slug}")
    public SalonPublicoRespuesta catalogo(@PathVariable String slug) {
        return catalogoService.obtenerCatalogo(slug);
    }

    @GetMapping("/{slug}/disponibilidad")
    public DisponibilidadRespuesta disponibilidad(@PathVariable String slug,
            @RequestParam Long servicioId, @RequestParam Long estilistaId, @RequestParam LocalDate fecha) {
        return catalogoService.obtenerDisponibilidad(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/{slug}/reservas")
    public ResponseEntity<CitaRespuesta> reservar(@PathVariable String slug,
            @Valid @RequestBody CitaRequest request) {
        CitaRespuesta respuesta = catalogoService.reservar(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/{slug}/chat")
    public ChatRespuesta chat(@PathVariable String slug, @Valid @RequestBody ChatRequest request) {
        return chatbotService.chatear(slug, request);
    }
}
