package com.devsenior.sillon.usuario.estilista;

import com.devsenior.sillon.security.UsuarioPrincipal;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estilista")
public class EstilistaController {
    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }
    @PostMapping("/crear")
    public ResponseEntity<EstilistaResponse> crearEstilista(@Valid @RequestBody EstilistaRequest estilistaResquest, @AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal){
        EstilistaResponse estilistaResponse = estilistaService.crearEstilista(estilistaResquest, usuarioPrincipal.getSalonId());
        return ResponseEntity.ok(estilistaResponse);
    }
    @GetMapping
    public ResponseEntity<List<EstilistaResponse>> getAllEstilistas(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal){
        List<EstilistaResponse> estilistas = estilistaService.obtenerEstilistasPorSalon(usuarioPrincipal.getSalonId());
        return ResponseEntity.ok(estilistas);
    }
}
