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

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioPeticion peticion) {
        return servicioService.crear(usuario.getSalonId(), peticion);
    }

    @GetMapping
    public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicioService.listar(usuario.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioRespuesta editar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody ServicioPeticion peticion) {
        return servicioService.editar(usuario.getSalonId(), id, peticion);
    }
}
