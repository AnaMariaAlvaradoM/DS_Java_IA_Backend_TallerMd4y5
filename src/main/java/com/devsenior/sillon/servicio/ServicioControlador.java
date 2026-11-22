package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

/**
 * Catalogo de servicios del salon.
 * Crear y editar: solo DUENO.
 * Listar: DUENO y ESTILISTA.
 */
@RestController
@RequestMapping("/api/servicios")
public class ServicioControlador {

    private final ServicioServicio servicioServicio;

    public ServicioControlador(ServicioServicio servicioServicio) {
        this.servicioServicio = servicioServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal principal,
                                   @Valid @RequestBody ServicioCreacionPeticion peticion) {
        return servicioServicio.crear(principal.getSalonId(), peticion);
    }

    @GetMapping
    public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return servicioServicio.listar(principal.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioRespuesta actualizar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ServicioCreacionPeticion peticion) {
        return servicioServicio.actualizar(principal.getSalonId(), id, peticion);
    }
}
