package com.devsenior.sillon.estilista;

import com.devsenior.sillon.estilista.dto.EstilistaRequestDto;
import com.devsenior.sillon.estilista.dto.EstilistaResponseDto;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<EstilistaResponseDto> crearEstilista(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody EstilistaRequestDto request) {

        EstilistaResponseDto response = estilistaService.crearEstilista(usuario.getSalonId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<List<EstilistaResponseDto>> listarEstilistas(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<EstilistaResponseDto> lista = estilistaService.listarEstilistas(usuario.getSalonId());
        return ResponseEntity.ok(lista);
    }
}