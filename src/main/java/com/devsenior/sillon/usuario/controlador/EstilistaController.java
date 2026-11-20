package com.devsenior.sillon.usuario.controlador;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.servicio.EstilistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @Valid @RequestBody EstilistaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estilistaService.crear(usuario.getSalonId(), request));
    }

    @GetMapping
    public ResponseEntity<List<EstilistaResponse>> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(estilistaService.listar(usuario.getSalonId()));
    }
}

