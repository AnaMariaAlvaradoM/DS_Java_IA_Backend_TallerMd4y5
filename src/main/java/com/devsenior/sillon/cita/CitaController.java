package com.devsenior.sillon.cita;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService citas;
    public CitaController(CitaService citas) { this.citas = citas; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CitaRequest request) {
        return citas.crear(principal.getSalonId(), request);
    }

    @GetMapping
    public List<CitaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) LocalDate fecha) {
        return citas.listar(principal, fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaResponse cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody EstadoCitaRequest request) {
        return citas.cambiarEstado(principal, id, request.estado());
    }
}
