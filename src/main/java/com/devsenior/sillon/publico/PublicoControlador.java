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

import com.devsenior.sillon.cita.CitaCreacionPeticion;
import com.devsenior.sillon.cita.CitaRespuesta;

import jakarta.validation.Valid;

/**
 * Endpoints publicos del salon (sin autenticacion requerida):
 * - GET /api/publico/salones/{slug}: catalogo de servicios y estilistas.
 * - GET /api/publico/salones/{slug}/disponibilidad: horarios libres para un servicio y estilista.
 * - POST /api/publico/salones/{slug}/reservas: reserva en linea (nace PENDIENTE y EN_LINEA).
 */
@RestController
@RequestMapping("/api/publico/salones/{slug}")
public class PublicoControlador {

    private final PublicoServicio publicoServicio;

    public PublicoControlador(PublicoServicio publicoServicio) {
        this.publicoServicio = publicoServicio;
    }

    @GetMapping
    public CatalogoPublicoRespuesta obtenerCatalogo(@PathVariable String slug) {
        return publicoServicio.obtenerCatalogo(slug);
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadRespuesta consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return publicoServicio.consultarDisponibilidad(slug, servicioId, estilistaId, fecha);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRespuesta reservar(
            @PathVariable String slug,
            @Valid @RequestBody CitaCreacionPeticion peticion) {
        return publicoServicio.reservarEnLinea(slug, peticion);
    }
}
