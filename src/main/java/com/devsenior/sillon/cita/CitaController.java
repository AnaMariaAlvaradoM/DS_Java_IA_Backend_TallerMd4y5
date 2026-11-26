package com.devsenior.sillon.cita;

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

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody CitaPeticion peticion) {
        return citaService.crearDesdeSalon(usuario.getSalonId(), peticion);
    }

    @GetMapping
    public List<CitaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) LocalDate fecha) {
        return citaService.listar(usuario, fecha);
    }

    @PatchMapping("/{id}/estado")
    public CitaRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody CambioEstadoPeticion peticion) {
        return citaService.cambiarEstado(usuario, id, peticion.estado());
    }
}
