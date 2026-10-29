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

import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaRespuesta;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
public class PublicoController {

    private final CatalogoService catalogoService;

    public PublicoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
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
}
