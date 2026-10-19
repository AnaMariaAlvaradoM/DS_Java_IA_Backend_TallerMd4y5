package com.devsenior.sillon.servicio;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
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

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    private final ServicioService servicios;
    public ServicioController(ServicioService servicios) { this.servicios = servicios; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioResponse crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ServicioRequest request) {
        return servicios.crear(principal.getSalonId(), request);
    }

    @GetMapping
    public List<ServicioResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return servicios.listar(principal.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioResponse editar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {
        return servicios.editar(principal.getSalonId(), id, request);
    }
}
