package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.dto.CitaResquest;
import com.devsenior.sillon.cita.dto.EstadoCitaDto;
import com.devsenior.sillon.security.UsuarioPrincipal;
import com.devsenior.sillon.usuario.model.Rol;
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
    public CitaController(CitaService citaService) {
        this.citaService = citaService;
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

    @PatchMapping("/{id}/recordatorio") // Ruta POST /api/citas/{id}/recordatorio
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<Object> enviarRecordatorio(@PathVariable Long id,
                                                      @AuthenticationPrincipal UsuarioPrincipal auth) {

        // Retorna 200 con detalles del recordatorio y factura[cite: 1]
        // TODO: Crear este método en CitaService en la fase correspondiente a IA
        // Object respuesta = citaService.enviarRecordatorioIa(id, auth.getSalonId());
        // return ResponseEntity.ok(respuesta);

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
