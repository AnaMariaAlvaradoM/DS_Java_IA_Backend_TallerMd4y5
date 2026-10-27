package com.devsenior.sillon.cita;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6 Agenda")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/citas")
public class RecordatorioController {
    private final RecordatorioService recordatorios;

    public RecordatorioController(RecordatorioService recordatorios) {
        this.recordatorios = recordatorios;
    }

    @PostMapping("/{id}/recordatorio")
    public RecordatorioResponse generar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return recordatorios.generar(principal.getSalonId(), id);
    }
}
