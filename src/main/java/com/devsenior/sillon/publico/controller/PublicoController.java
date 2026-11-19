package com.devsenior.sillon.publico.controller;

import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.DisponibilidadResponse;
import com.devsenior.sillon.cita.service.CitaService;
import com.devsenior.sillon.publico.dto.CatalogoResponse;
import com.devsenior.sillon.publico.service.CatalogoService;
import com.devsenior.sillon.salon.service.SalonService;
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

/** Lo que ve el cliente final sin iniciar sesión. El salón se identifica por su slug. */
@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoController {

    private final CatalogoService catalogoService;
    private final CitaService citaService;
    private final SalonService salonService;

    public PublicoController(CatalogoService catalogoService, CitaService citaService, SalonService salonService) {
        this.catalogoService = catalogoService;
        this.citaService = citaService;
        this.salonService = salonService;
    }

    @GetMapping
    public CatalogoResponse catalogo(@PathVariable String slug) {
        return catalogoService.catalogo(slug);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@PathVariable String slug,
                                                 @RequestParam Long servicioId,
                                                 @RequestParam Long estilistaId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Long salonId = salonService.obtenerPorSlug(slug).getId();
        return citaService.disponibilidad(salonId, servicioId, estilistaId, fecha);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse reservar(@PathVariable String slug, @Valid @RequestBody CitaRequest request) {
        Long salonId = salonService.obtenerPorSlug(slug).getId();
        return citaService.reservarEnLinea(salonId, request);
    }
}
