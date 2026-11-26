package com.devsenior.sillon.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public EstilistaRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody EstilistaPeticion peticion) {
        return estilistaService.crear(usuario.getSalonId(), peticion);
    }

    @GetMapping
    public List<EstilistaRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return estilistaService.listar(usuario.getSalonId());
    }
}
