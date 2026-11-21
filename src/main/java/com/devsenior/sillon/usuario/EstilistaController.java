package com.devsenior.sillon.usuario;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estilistas")
public class EstilistaController {
    private final EstilistaService servicio;

    public EstilistaController(EstilistaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<EstilistaResponse> crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                   @Valid @RequestBody EstilistaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuario.getSalonId(), request));
    }

    @GetMapping
    public List<EstilistaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.listar(usuario.getSalonId());
    }
}
