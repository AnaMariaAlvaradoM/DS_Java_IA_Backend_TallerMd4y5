
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

    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }

    @PostMapping
    public ResponseEntity<EstilistaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody EstilistaRequest request) {

        EstilistaResponse respuesta = estilistaService.crear(
                usuario.getSalonId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EstilistaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<EstilistaResponse> estilistas =
                estilistaService.listar(usuario.getSalonId());

        return ResponseEntity.ok(estilistas);
    }
}
