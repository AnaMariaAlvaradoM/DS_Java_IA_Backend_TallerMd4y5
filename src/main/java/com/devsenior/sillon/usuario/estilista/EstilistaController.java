package com.devsenior.sillon.usuario.estilista;

import com.devsenior.sillon.security.UsuarioPrincipal;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaRequest;
import com.devsenior.sillon.usuario.estilista.dto.EstilistaResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estilistas")
@Tag(name = "4 Estilistas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DUENO')")
public class EstilistaController {
    private final EstilistaService estilistaService;

    public EstilistaController(EstilistaService estilistaService) {
        this.estilistaService = estilistaService;
    }
    @PostMapping
    public ResponseEntity<EstilistaResponse> crearEstilista(@Valid @RequestBody EstilistaRequest estilistaResquest, @AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal){
        System.out.println("========== AUTENTICACIÓN ==========");
        System.out.println("Usuario ID: " + usuarioPrincipal.getId());
        System.out.println("Correo: " + usuarioPrincipal.getEmail());
        System.out.println("Rol: " + usuarioPrincipal.getRol());
        System.out.println("Salón ID: " + usuarioPrincipal.getSalonId());
        EstilistaResponse estilistaResponse = estilistaService.crearEstilista(estilistaResquest, usuarioPrincipal.getSalonId());
        return ResponseEntity.status(201).body(estilistaResponse);
    }
    @GetMapping
    public ResponseEntity<List<EstilistaResponse>> getAllEstilistas(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal){
        List<EstilistaResponse> estilistas = estilistaService.obtenerEstilistasPorSalon(usuarioPrincipal.getSalonId());
        return ResponseEntity.ok(estilistas);
    }

}
