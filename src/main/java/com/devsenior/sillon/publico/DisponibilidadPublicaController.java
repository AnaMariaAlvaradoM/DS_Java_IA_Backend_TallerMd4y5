
package com.devsenior.sillon.publico;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/publico/salones")
@Tag(
    name = "Disponibilidad pública",
    description = "Consulta de horarios disponibles para reservas sin autenticación"
)
public class DisponibilidadPublicaController {

    private final DisponibilidadPublicaService disponibilidadService;

    public DisponibilidadPublicaController(
            DisponibilidadPublicaService disponibilidadService
    ) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/{slug}/disponibilidad")
    public DisponibilidadPublicaResponse consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha
    ) {
        return disponibilidadService.consultarDisponibilidad(
                slug,
                servicioId,
                estilistaId,
                fecha
        );
    }
}
