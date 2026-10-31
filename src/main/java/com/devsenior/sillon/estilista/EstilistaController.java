package com.devsenior.sillon.estilista;

import com.devsenior.sillon.estilista.dto.EstilistaRequestDto;
import com.devsenior.sillon.estilista.dto.EstilistaResponseDto;
import com.devsenior.sillon.estilista.dto.HorarioEstilistaDto;
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
    public ResponseEntity<List<EstilistaResponseDto>> listarEstilistas(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<EstilistaResponseDto> lista = estilistaService.listarEstilistas(usuario.getSalonId());
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<EstilistaResponseDto> actualizarEstilista(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody EstilistaRequestDto request) {

        EstilistaResponseDto response = estilistaService.actualizarEstilista(id, usuario.getSalonId(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<Void> desactivarEstilista(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        estilistaService.cambiarEstado(id, usuario.getSalonId(), false);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}/horarios")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<List<HorarioEstilistaDto>> asignarHorarios(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody List<HorarioEstilistaDto> horarios) {

        List<HorarioEstilistaDto> response = estilistaService.asignarHorarios(id, usuario.getSalonId(), horarios);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<List<HorarioEstilistaDto>> obtenerHorarios(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        List<HorarioEstilistaDto> response = estilistaService.obtenerHorarios(id, usuario.getSalonId());
        return ResponseEntity.ok(response);
    }
}