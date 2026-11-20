package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CambiarEstadoCitaRequest;
import com.devsenior.sillon.cita.dto.CitaRespuesta;
import com.devsenior.sillon.cita.dto.CrearCitaRequest;
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

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    public ResponseEntity<CitaRespuesta> agendarCita(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearCitaRequest request
    ) {
        CitaRespuesta respuesta = citaService.agendarCitaSalon(principal.getSalonId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<CitaRespuesta>> listarCitas(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<CitaRespuesta> citas = citaService.listarCitasPorDia(principal, fecha);
        return ResponseEntity.ok(citas);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaRespuesta> cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoCitaRequest request
    ) {
        CitaRespuesta respuesta = citaService.cambiarEstadoCita(principal, id, request);
        return ResponseEntity.ok(respuesta);
    }
}
