package com.devsenior.sillon.cita;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

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
    public ResponseEntity<CitaRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CitaRequest request) {
        CitaRespuesta respuesta = citaService.agendar(principal.getSalonId(), request, EstadoCita.CONFIRMADA,
                OrigenCita.SALON);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public List<CitaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) LocalDate fecha) {
        return citaService.listarPorDia(principal.getSalonId(), principal.getId(), principal.getRol(), fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return citaService.cambiarEstado(principal.getSalonId(), id, principal.getId(), principal.getRol(),
                request.estado());
    }

    @PostMapping("/{id}/recordatorio")
    public RecordatorioRespuesta recordatorio(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return recordatorioService.generar(principal.getSalonId(), id);
    }
}
