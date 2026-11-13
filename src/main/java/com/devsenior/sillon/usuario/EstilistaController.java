package com.devsenior.sillon.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estilistas")
public class EstilistaController {

    private final EstilistaServicio estilistaServicio;

    public EstilistaController(EstilistaServicio estilistaServicio) {
        this.estilistaServicio = estilistaServicio;
    }

    @PostMapping
    public ResponseEntity<EstilistaRespuesta> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody EstilistaSolicitud solicitud) {
        EstilistaRespuesta respuesta = estilistaServicio.crear(principal.getSalonId(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EstilistaRespuesta>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(estilistaServicio.listar(principal.getSalonId()));
    }
}
