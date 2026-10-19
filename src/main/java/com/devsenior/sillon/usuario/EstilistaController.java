package com.devsenior.sillon.usuario;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
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

@RestController
@RequestMapping("/api/estilistas")
public class EstilistaController {
    private final EstilistaService estilistas;
    public EstilistaController(EstilistaService estilistas) { this.estilistas = estilistas; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstilistaResponse crear(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody EstilistaRequest request) {
        return estilistas.crear(principal.getSalonId(), request);
    }

    @GetMapping
    public List<EstilistaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return estilistas.listar(principal.getSalonId());
    }
}
