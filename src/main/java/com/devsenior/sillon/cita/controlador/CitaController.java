package com.devsenior.sillon.cita.controlador;

import com.devsenior.sillon.cita.dto.CambiarEstadoCitaRequest;
import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.cita.servicio.CitaService;
import com.devsenior.sillon.ia.servicio.RecordatorioIaService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;
    private final RecordatorioIaService recordatorioIaService;

    public CitaController(CitaService citaService, RecordatorioIaService recordatorioIaService) {
        this.citaService = citaService;
        this.recordatorioIaService = recordatorioIaService;
    }

    @PostMapping
    public ResponseEntity<CitaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citaService.crearDesdeSalon(usuario.getSalonId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CitaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.listarPorDia(usuario, fecha));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaResponse> cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoCitaRequest request) {
        return ResponseEntity.ok(citaService.cambiarEstado(usuario, id, request.estado()));
    }

    @PostMapping("/{id}/recordatorio")
    public ResponseEntity<RecordatorioResponse> generarRecordatorio(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id
    ) {
       return ResponseEntity.ok(recordatorioIaService.generar(usuario.getSalonId(), id));
    }
}