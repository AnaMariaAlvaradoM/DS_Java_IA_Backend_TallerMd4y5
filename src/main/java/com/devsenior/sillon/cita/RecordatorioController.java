package com.devsenior.sillon.cita;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

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
