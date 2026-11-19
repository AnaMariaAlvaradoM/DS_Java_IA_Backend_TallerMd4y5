package com.devsenior.sillon.servicio.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.servicio.service.ServicioService;
import jakarta.validation.Valid;
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

@Tag(name = "3 Servicios", description = "Catálogo de servicios del salón")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioResponse crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                 @Valid @RequestBody ServicioRequest request) {
        return servicioService.crear(usuario.getSalonId(), request);
    }

    @GetMapping
    public List<ServicioResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicioService.listar(usuario.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ServicioRequest request) {
        return servicioService.actualizar(usuario.getSalonId(), id, request);
    }
}
