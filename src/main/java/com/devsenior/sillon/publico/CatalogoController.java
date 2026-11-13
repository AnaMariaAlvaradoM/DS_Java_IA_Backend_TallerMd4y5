package com.devsenior.sillon.publico;

import com.devsenior.sillon.chatbot.dto.ChatbotRequest;
import com.devsenior.sillon.chatbot.dto.ChatbotResponse;
import com.devsenior.sillon.chatbot.service.ChatbotService;
import com.devsenior.sillon.cita.service.CitaService;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.CitaResquest;
import com.devsenior.sillon.publico.dto.CatalogoResponse;
import com.devsenior.sillon.publico.dto.DisponibilidadResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final CitaService citaService;
    private final ChatbotService chatbotService;


    public CatalogoController(CatalogoService catalogoService, CitaService citaService, ChatbotService chatbotService) {
        this.catalogoService = catalogoService;
        this.citaService = citaService;
        this.chatbotService = chatbotService;
    }

    @GetMapping
    public ResponseEntity<CatalogoResponse> obtenerCatalogo(@PathVariable String slug) {
        return ResponseEntity.ok(catalogoService.obtenerCatalogo(slug));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadResponse> obtenerDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return ResponseEntity.ok(catalogoService.obtenerDisponibilidad(slug, servicioId, estilistaId, fecha));
    }

    @PostMapping("/reservas")
    public ResponseEntity<CitaResponse> reservarEnLinea(
            @PathVariable String slug,
            @Valid @RequestBody CitaResquest dto) {

        CitaResponse response = citaService.reservarEnLinea(slug, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatbotResponse> procesarChat(
            @PathVariable("slug") String slug,
            @Valid @RequestBody ChatbotRequest request) {

        ChatbotResponse response = chatbotService.procesarMensaje(
                slug,
                request.clienteNombre(),
                request.clienteContacto(),
                null, // o recuperar la dirección si el servicio la requiere como parámetro
                request.historial()
        );

        return ResponseEntity.ok(response);
    }
}
