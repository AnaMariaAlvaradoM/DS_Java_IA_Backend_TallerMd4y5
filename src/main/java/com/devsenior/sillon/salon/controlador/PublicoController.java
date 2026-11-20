package com.devsenior.sillon.salon.controlador;

import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.servicio.CitaService;
import com.devsenior.sillon.salon.dto.CatalogoPublicoResponse;
import com.devsenior.sillon.salon.dto.DisponibilidadResponse;
import com.devsenior.sillon.salon.servicio.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    public ResponseEntity<CatalogoPublicoResponse> obtenerCatalogo(@PathVariable String slug) {
        return ResponseEntity.ok(catalogoService.obtenerCatalogo(slug));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadResponse> consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(catalogoService.consultarDisponibilidad(slug, servicioId, estilistaId, fecha));
    }

    @PostMapping("/reservas")
    public ResponseEntity<CitaResponse> reservarEnLinea(
            @PathVariable String slug,
            @Valid @RequestBody CitaRequest request) {
        Long salonId = catalogoService.obtenerSalonIdPorSlug(slug);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citaService.crearReservaEnLinea(salonId, request));
    }
}