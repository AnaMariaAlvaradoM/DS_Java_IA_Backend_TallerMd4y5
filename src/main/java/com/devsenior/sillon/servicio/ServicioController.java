package com.devsenior.sillon.servicio;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.CrearServicioRequest;
import com.devsenior.sillon.servicio.dto.ServicioRespuesta;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ServicioRespuesta> crearServicio(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearServicioRequest request
    ) {
        ServicioRespuesta respuesta = servicioService.crearServicio(principal.getSalonId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<ServicioRespuesta>> listarServicios(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        List<ServicioRespuesta> servicios = servicioService.listarServicios(principal.getSalonId());
        return ResponseEntity.ok(servicios);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioRespuesta> actualizarServicio(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CrearServicioRequest request
    ) {
        ServicioRespuesta respuesta = servicioService.actualizarServicio(principal.getSalonId(), id, request);
        return ResponseEntity.ok(respuesta);
    }
}
