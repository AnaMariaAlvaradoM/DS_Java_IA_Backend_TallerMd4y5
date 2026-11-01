package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.CitaService;
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
    private final CitaService citaService; // Inyectas CitaService

    public CatalogoController(CatalogoService catalogoService, CitaService citaService) {
        this.catalogoService = catalogoService;
        this.citaService = citaService;
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
}
