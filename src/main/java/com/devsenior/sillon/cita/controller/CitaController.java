package com.devsenior.sillon.cita.controller;

import com.devsenior.sillon.cita.dto.CambioEstadoRequest;
import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.service.CitaService;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                              @Valid @RequestBody CitaRequest request) {
        return citaService.crearDesdeSalon(usuario.getSalonId(), request);
    }

    @GetMapping
    public List<CitaResponse> agendaDelDia(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citaService.listarDelDia(usuario.getSalonId(), usuario.getId(), usuario.esEstilista(), fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaResponse cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CambioEstadoRequest request) {
        return citaService.cambiarEstado(usuario.getSalonId(), usuario.getId(), usuario.esEstilista(),
                id, request.estado());
    }
}
