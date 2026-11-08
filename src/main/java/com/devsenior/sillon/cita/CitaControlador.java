package com.devsenior.sillon.cita;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/citas")
public class CitaControlador {
    private final CitaServicio servicio;

    public CitaControlador(CitaServicio servicio) { this.servicio = servicio; }

    @PostMapping
    public ResponseEntity<CitaRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody CitaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.agendarSalon(usuario.getSalonId(), solicitud));
    }

    @GetMapping
    public List<CitaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.listar(usuario, fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody EstadoCitaSolicitud solicitud) {
        return servicio.cambiarEstado(usuario, id, solicitud.estado());
    }
}
