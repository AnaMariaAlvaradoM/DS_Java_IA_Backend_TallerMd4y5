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
public class ServicioControlador {
    private final ServicioServicio servicio;

    public ServicioControlador(ServicioServicio servicio) { this.servicio = servicio; }

    @PostMapping
    public ResponseEntity<ServicioRespuesta> crear(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ServicioSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuario.getSalonId(), solicitud));
    }

    @GetMapping
    public List<ServicioRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.listar(usuario.getSalonId());
    }

    @PutMapping("/{id}")
    public ServicioRespuesta actualizar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody ServicioSolicitud solicitud) {
        return servicio.actualizar(usuario.getSalonId(), id, solicitud);
    }
}
