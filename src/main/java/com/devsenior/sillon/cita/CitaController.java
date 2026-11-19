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

import com.devsenior.sillon.cita.dto.CambioEstadoRequest;
import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citas;

    public CitaController(CitaService citas) {
        this.citas = citas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse agendar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CitaRequest peticion) {
        return citas.agendarEnSalon(principal.getSalonId(), peticion);
    }

    @GetMapping
    public List<CitaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citas.listar(principal, fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaResponse cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest peticion) {
        return citas.cambiarEstado(principal, id, peticion.estado());
    }
}
