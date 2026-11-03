package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.dto.CitaRequestDto;
import com.devsenior.sillon.cita.dto.CitaResponseDto;
import com.devsenior.sillon.publico.dto.DisponibilidadDto;
import com.devsenior.sillon.publico.dto.SalonCatalogoResponseDto;
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
    public ResponseEntity<SalonCatalogoResponseDto> obtenerCatalogo(@PathVariable String slug) {
        SalonCatalogoResponseDto catalogo = catalogoService.obtenerCatalogo(slug);
        return ResponseEntity.ok(catalogo);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadDto> consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        DisponibilidadDto disponibilidad = catalogoService.calcularDisponibilidad(
                slug,
                servicioId,
                estilistaId,
                fecha
        );
        return ResponseEntity.ok(disponibilidad);
    }

    @PostMapping("/reservas")
    public ResponseEntity<CitaResponseDto> reservarEnLinea(
            @PathVariable String slug,
            @Valid @RequestBody CitaRequestDto request) {

        CitaResponseDto cita = citaService.reservarEnLinea(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cita);
    }
}
