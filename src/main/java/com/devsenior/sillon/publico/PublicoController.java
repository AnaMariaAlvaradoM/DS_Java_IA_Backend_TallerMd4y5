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

import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaSolicitud;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
public class PublicoController {

    private final PublicoServicio publicoServicio;

    public PublicoController(PublicoServicio publicoServicio) {
        this.publicoServicio = publicoServicio;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CatalogoPublicoRespuesta> catalogo(@PathVariable String slug) {
        return ResponseEntity.ok(publicoServicio.catalogo(slug));
    }

    @GetMapping("/{slug}/disponibilidad")
    public ResponseEntity<DisponibilidadRespuesta> disponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(
                publicoServicio.disponibilidad(slug, servicioId, estilistaId, fecha));
    }

    @PostMapping("/{slug}/reservas")
    public ResponseEntity<CitaRespuesta> reservar(@PathVariable String slug,
            @Valid @RequestBody CitaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicoServicio.reservar(slug, solicitud));
    }
}
