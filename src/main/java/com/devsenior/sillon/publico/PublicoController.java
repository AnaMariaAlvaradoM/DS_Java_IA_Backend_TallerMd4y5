package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.dto.CitaRespuesta;
import com.devsenior.sillon.cita.dto.CrearCitaRequest;
import com.devsenior.sillon.publico.dto.CatalogoSalonRespuesta;
import com.devsenior.sillon.publico.dto.DisponibilidadRespuesta;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoController {

    private final PublicoService publicoService;

    public PublicoController(PublicoService publicoService) {
        this.publicoService = publicoService;
    }

    @GetMapping
    public ResponseEntity<CatalogoSalonRespuesta> obtenerCatalogo(@PathVariable String slug) {
        CatalogoSalonRespuesta catalogo = publicoService.obtenerCatalogo(slug);
        return ResponseEntity.ok(catalogo);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadRespuesta> consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        DisponibilidadRespuesta respuesta = publicoService.consultarDisponibilidad(slug, servicioId, estilistaId, fecha);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/reservas")
    public ResponseEntity<CitaRespuesta> crearReserva(
            @PathVariable String slug,
            @Valid @RequestBody CrearCitaRequest request
    ) {
        CitaRespuesta cita = publicoService.crearReservaEnLinea(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cita);
    }
}
