package com.devsenior.sillon.usuario;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.dto.CrearEstilistaRequest;
import com.devsenior.sillon.usuario.dto.EstilistaRespuesta;
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
    public ResponseEntity<EstilistaRespuesta> crearEstilista(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearEstilistaRequest request
    ) {
        EstilistaRespuesta respuesta = estilistaService.crearEstilista(principal.getSalonId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EstilistaRespuesta>> listarEstilistas(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        List<EstilistaRespuesta> estilistas = estilistaService.listarEstilistas(principal.getSalonId());
        return ResponseEntity.ok(estilistas);
    }
}
