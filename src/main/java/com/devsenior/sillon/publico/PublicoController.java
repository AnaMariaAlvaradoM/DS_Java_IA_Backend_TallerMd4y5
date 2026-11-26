package com.devsenior.sillon.publico;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cita.CitaPeticion;
import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaService;

import jakarta.validation.Valid;

/** Rutas públicas (sin token) del salón. */
@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoController {

    private final CatalogoService catalogoService;
    private final CitaService citaService;

    public PublicoController(CatalogoService catalogoService, CitaService citaService) {
        this.catalogoService = catalogoService;
        this.citaService = citaService;
    }

    @GetMapping
    public CatalogoPublico catalogo(@PathVariable String slug) {
        return catalogoService.catalogo(slug);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadRespuesta disponibilidad(@PathVariable String slug, @RequestParam Long servicioId,
            @RequestParam Long estilistaId, @RequestParam LocalDate fecha) {
        return catalogoService.disponibilidad(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRespuesta reservar(@PathVariable String slug, @Valid @RequestBody CitaPeticion peticion) {
        return citaService.reservarEnLinea(slug, peticion);
    }
}
