package com.devsenior.sillon.publico;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/salones")
public class PublicoController {

    private final PublicoService servicio;
    private final CitaService citas;

    public PublicoController(PublicoService servicio, CitaService citas) {
        this.servicio = servicio;
        this.citas = citas;
    }

    @GetMapping("/{slug}")
    public CatalogoPublicoResponse catalogo(@PathVariable String slug) {
        return servicio.catalogo(slug);
    }

    @GetMapping("/{slug}/disponibilidad")
    public DisponibilidadPublicaResponse disponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.disponibilidad(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/{slug}/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse reservar(@PathVariable String slug, @Valid @RequestBody CitaRequest peticion) {
        return citas.reservarEnLinea(slug, peticion);
    }
}
