
package com.devsenior.sillon.ia.controlador;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.ia.dto.RespuestaRecordatorioIa;
import com.devsenior.sillon.ia.servicio.RecordatorioIaServicio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "6 Agenda", description = "Generación de recordatorios de citas mediante IA")
@SecurityRequirement(name = "Bearer JWT")
public class RecordatorioIaControlador {

    private final RecordatorioIaServicio recordatorioIaServicio;

    public RecordatorioIaControlador(
            RecordatorioIaServicio recordatorioIaServicio) {

        this.recordatorioIaServicio = recordatorioIaServicio;
    }

    @PostMapping("/{id}/recordatorio")
    public ResponseEntity<RespuestaRecordatorioIa> generar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id) {

        RespuestaRecordatorioIa respuesta = recordatorioIaServicio.generar(
                usuario.getSalonId(),
                id);

        return ResponseEntity.ok(respuesta);
    }
}
