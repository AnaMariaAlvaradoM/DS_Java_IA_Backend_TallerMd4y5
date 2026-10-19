package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaResponse;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoController {
    private final CatalogoService catalogo;
    public PublicoController(CatalogoService catalogo) { this.catalogo = catalogo; }

    @GetMapping
    public CatalogoResponse consultar(@PathVariable String slug) { return catalogo.consultar(slug); }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@PathVariable String slug, @RequestParam Long servicioId,
            @RequestParam Long estilistaId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogo.disponibilidad(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse reservar(@PathVariable String slug, @Valid @RequestBody CitaRequest request) {
        return catalogo.reservar(slug, request);
    }
}
