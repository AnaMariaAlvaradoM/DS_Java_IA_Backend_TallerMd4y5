package com.devsenior.sillon.usuario.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import com.devsenior.sillon.usuario.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaResponse;
import com.devsenior.sillon.usuario.service.EstilistaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4 Estilistas", description = "Equipo de estilistas y sus servicios")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/estilistas")
public class EstilistaController {

    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstilistaResponse crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                   @Valid @RequestBody EstilistaRequest request) {
        return estilistaService.crear(usuario.getSalonId(), request);
    }

    @GetMapping
    public List<EstilistaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return estilistaService.listar(usuario.getSalonId());
    }
}
