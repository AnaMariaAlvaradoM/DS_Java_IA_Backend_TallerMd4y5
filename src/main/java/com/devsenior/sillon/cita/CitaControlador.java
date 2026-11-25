package com.devsenior.sillon.cita;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

/**
 * Endpoints de la agenda de citas:
 * - POST /api/citas (solo DUENO): crea cita confirmada.
 * - GET /api/citas?fecha= (DUENO y ESTILISTA): lista citas del dia.
 * - PATCH /api/citas/{id}/estado (DUENO y ESTILISTA): actualiza estado de cita.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaControlador {

    private final CitaServicio citaServicio;

    public CitaControlador(CitaServicio citaServicio) {
        this.citaServicio = citaServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                               @Valid @RequestBody CitaCreacionPeticion peticion) {
        return citaServicio.crearPorSalon(usuario.getSalonId(), peticion);
    }

    @GetMapping
    public List<CitaRespuesta> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaServicio.listarPorDia(usuario, fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaRespuesta cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody CitaCambioEstadoPeticion peticion) {
        return citaServicio.cambiarEstado(usuario, id, peticion.estado());
    }

    @PostMapping("/{id}/recordatorio")
    @ResponseStatus(HttpStatus.OK)
    public RecordatorioRespuesta generarRecordatorio(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return citaServicio.generarRecordatorio(id, usuario.getSalonId());
    }
}
