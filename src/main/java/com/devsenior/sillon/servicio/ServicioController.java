
package com.devsenior.sillon.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ServicioResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioRequest request) {

        ServicioResponse respuesta = servicioService.crear(
                usuario.getSalonId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<ServicioResponse> servicios =
                servicioService.listar(usuario.getSalonId());

        return ResponseEntity.ok(servicios);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {

        ServicioResponse respuesta = servicioService.actualizar(
                usuario.getSalonId(),
                id,
                request
        );

        return ResponseEntity.ok(respuesta);
    }
}
