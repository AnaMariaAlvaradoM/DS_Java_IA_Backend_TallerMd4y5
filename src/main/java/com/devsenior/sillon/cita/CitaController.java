
package com.devsenior.sillon.cita;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
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


}
