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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estilistas")
@Tag(name = "4 Estilistas")
@SecurityRequirement(name = "bearerAuth")
public class EstilistaController {

    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }

    @PostMapping
    public ResponseEntity<EstilistaRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody EstilistaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estilistaService.crear(principal.getSalonId(), request));
    }

    @GetMapping
    public List<EstilistaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return estilistaService.listarPorSalon(principal.getSalonId());
    }
}
