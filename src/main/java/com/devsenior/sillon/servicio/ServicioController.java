package com.devsenior.sillon.servicio;

import com.devsenior.sillon.security.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.ServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }
    @GetMapping
    @PreAuthorize("hasRole('DUENO') or hasRole('ESTILISTA')")
    public ResponseEntity<List<ServicioResponse>> listarServicios(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        Long salonId = usuario.getSalonId();
        List<ServicioResponse> servicios = servicioService.obtenerServicio(salonId);
        return ResponseEntity.ok(servicios);
    }
    @PostMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ServicioResponse> crear(
            @Valid @RequestBody ServicioRequest dto,
            @AuthenticationPrincipal UsuarioPrincipal usuario
    ) {
        ServicioResponse respuesta = servicioService.crearServicio(dto, usuario.getSalonId());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ServicioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest dto,
            @AuthenticationPrincipal UsuarioPrincipal usuario
    ) {
        ServicioResponse respuesta = servicioService.actualizar(id, dto, usuario.getSalonId());
        return ResponseEntity.ok(respuesta);
    }

}
