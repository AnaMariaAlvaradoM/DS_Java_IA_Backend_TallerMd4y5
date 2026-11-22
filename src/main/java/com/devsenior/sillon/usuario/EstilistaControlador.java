package com.devsenior.sillon.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

/**
 * Gestion de estilistas del salon (solo DUENO).
 * El salonId sale del usuario autenticado.
 */
@RestController
@RequestMapping("/api/estilistas")
public class EstilistaControlador {

    private final EstilistaServicio estilistaServicio;

    public EstilistaControlador(EstilistaServicio estilistaServicio) {
        this.estilistaServicio = estilistaServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstilistaRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal principal,
                                   @Valid @RequestBody EstilistaCreacionPeticion peticion) {
        return estilistaServicio.crear(principal.getSalonId(), peticion);
    }

    @GetMapping
    public List<EstilistaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return estilistaServicio.listar(principal.getSalonId());
    }
}
