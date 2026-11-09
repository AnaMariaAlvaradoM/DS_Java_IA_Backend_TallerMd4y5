package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CambioEstadoRequestDto;
import com.devsenior.sillon.cita.dto.CitaResponseDto;
import com.devsenior.sillon.cita.dto.RecordatorioRespuestaDto;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

    @PostMapping("/{id}/recordatorio")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<RecordatorioRespuestaDto> generarRecordatorio(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        RecordatorioRespuestaDto respuesta = recordatorioService.generarRecordatorio(id, principal.getSalonId());
        return ResponseEntity.ok(respuesta);
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','ESTILISTA')")
    public ResponseEntity<List<CitaResponseDto>> listarPorDia(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<CitaResponseDto> citas = citaService.listarPorDia(
                usuario.getSalonId(),
                usuario.getId(),
                Rol.valueOf(usuario.getRol()),
                fecha
        );
        return ResponseEntity.ok(citas);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('DUENO','ESTILISTA')")
    public ResponseEntity<CitaResponseDto> cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequestDto request) {

        CitaResponseDto cita = citaService.cambiarEstado(
                usuario.getSalonId(),
                usuario.getId(),
                Rol.valueOf(usuario.getRol()),
                id,
                request.estado()
        );
        return ResponseEntity.ok(cita);
    }
}