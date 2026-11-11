package com.devsenior.sillon.cita.controller;

import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.CitaResquest;
import com.devsenior.sillon.cita.dto.EstadoCitaDto;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.cita.service.CitaService;
import com.devsenior.sillon.cita.service.RecordatorioService;
import com.devsenior.sillon.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService citaService;
    private final RecordatorioService recordatorioService;

    public CitaController(CitaService citaService, RecordatorioService recordatorioService) {
        this.citaService = citaService;
        this.recordatorioService = recordatorioService;
    }
    @PostMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<CitaResponse> agendar(@Valid @RequestBody CitaResquest dto,
                                                @AuthenticationPrincipal UsuarioPrincipal auth){
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.agendarDesdeSalon(dto, auth.getSalonId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO', 'ESTILISTA')")
    public ResponseEntity<List<CitaResponse>> listarPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @AuthenticationPrincipal UsuarioPrincipal auth) {

        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<CitaResponse> citas = citaService.listarPorDia(auth.getSalonId(), fecha, auth.getId(),auth.getRol());
        return ResponseEntity.ok(citas);
    }

    @PatchMapping("/{id}/estado") // Ruta PATCH /api/citas/{id}/estado[cite: 1]
    @PreAuthorize("hasAnyRole('DUENO', 'ESTILISTA')") // DUENO y ESTILISTA[cite: 1]
    public ResponseEntity<CitaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoCitaDto dto, // Recibe estado[cite: 1]
            @AuthenticationPrincipal UsuarioPrincipal auth) {

        return ResponseEntity.ok(citaService.cambiarEstado(
                id, auth.getSalonId(), dto.estado(), auth.getId(), auth.getRol()));
    }

    @PostMapping("/{citaId}/recordatorio") // Ruta POST /api/citas/{id}/recordatorio
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<RecordatorioResponse> generarRecordatorio(
            @PathVariable Long citaId,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        RecordatorioResponse response = recordatorioService.generarRecordatorio(usuario.getSalonId(), citaId);
        return ResponseEntity.ok(response);
    }
}
