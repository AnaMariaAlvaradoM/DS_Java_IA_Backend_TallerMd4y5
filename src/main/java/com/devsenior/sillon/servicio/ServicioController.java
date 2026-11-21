package com.devsenior.sillon.servicio;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                    @Valid @RequestBody ServicioSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioService.crear(usuario.getSalonId(), solicitud));
    }

    @GetMapping
    public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicioService.listar(usuario.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioRespuesta actualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                        @PathVariable Long id, @Valid @RequestBody ServicioSolicitud solicitud) {
        return servicioService.actualizar(usuario.getSalonId(), id, solicitud);
    }
}
