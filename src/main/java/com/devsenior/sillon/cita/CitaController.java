
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

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.ia.ServicioRecordatorioIA;
import com.devsenior.sillon.ia.RespuestaRecordatorioIA;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citas")
@Tag(
    name = "Citas",
    description = "Gestión de agenda, estados de citas y recordatorios con inteligencia artificial"
)
public class CitaController {

    private final CitaService citaService;
    private final ServicioRecordatorioIA servicioRecordatorioIA;

    public CitaController(
            CitaService citaService,
            ServicioRecordatorioIA servicioRecordatorioIA
    ) {
        this.citaService = citaService;
        this.servicioRecordatorioIA = servicioRecordatorioIA;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse crear(
            @AuthenticationPrincipal(expression = "salonId")
            Long salonId,
            @Valid @RequestBody CitaRequest request
    ) {
        return citaService.crear(salonId, request);
    }

    @GetMapping
    public List<CitaResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha
    ) {
        return citaService.listarPorFecha(
                usuario.getSalonId(),
                usuario.getId(),
                usuario.getRol(),
                fecha
        );
    }

    @PatchMapping("/{id}/estado")
    public CitaResponse actualizarEstado(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoCitaRequest request
    ) {
        return citaService.actualizarEstado(
                usuario.getSalonId(),
                usuario.getId(),
                usuario.getRol(),
                id,
                request
        );
    }

    @PostMapping("/{id}/recordatorio")
    public RespuestaRecordatorioIA generarRecordatorio(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id
    ) {
        return servicioRecordatorioIA.generar(
                usuario.getSalonId(),
                id
        );
    }
}
