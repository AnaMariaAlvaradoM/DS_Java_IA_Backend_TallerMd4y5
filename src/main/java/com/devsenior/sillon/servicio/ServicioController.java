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

    private final CatalogoServicio catalogoServicio;

    public ServicioController(CatalogoServicio catalogoServicio) {
        this.catalogoServicio = catalogoServicio;
    }

    @PostMapping
    public ResponseEntity<ServicioRespuesta> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ServicioSolicitud solicitud) {
        ServicioRespuesta respuesta = catalogoServicio.crear(principal.getSalonId(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<ServicioRespuesta>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(catalogoServicio.listar(principal.getSalonId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioRespuesta> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ServicioSolicitud solicitud) {
        return ResponseEntity.ok(catalogoServicio.actualizar(principal.getSalonId(), id, solicitud));
    }
}
