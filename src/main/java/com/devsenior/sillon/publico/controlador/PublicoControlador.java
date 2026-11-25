
package com.devsenior.sillon.publico.controlador;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaDisponibilidad;
import com.devsenior.sillon.publico.servicio.PublicoServicio;

@RestController
@RequestMapping("/api/publico/salones")
public class PublicoControlador {

    private final PublicoServicio publicoServicio;

    public PublicoControlador(PublicoServicio publicoServicio) {
        this.publicoServicio = publicoServicio;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<RespuestaCatalogoPublico> consultarCatalogo(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                publicoServicio.consultarCatalogo(slug));
    }

    @GetMapping("/{slug}/disponibilidad")
    public ResponseEntity<RespuestaDisponibilidad> consultarDisponibilidad(
            @PathVariable String slug,
            @RequestParam Long servicioId,
            @RequestParam Long estilistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return ResponseEntity.ok(
                publicoServicio.consultarDisponibilidad(
                        slug, servicioId, estilistaId, fecha));
    }

    @PostMapping("/{slug}/reservas")
    public ResponseEntity<RespuestaCita> reservar(
            @PathVariable String slug,
            @Valid @RequestBody SolicitudCrearCita solicitud) {

        RespuestaCita respuesta = publicoServicio.reservar(slug, solicitud);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
