
package com.devsenior.sillon.autenticacion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;

    public ControladorAutenticacion(
            ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaAutenticacion> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud) {

        RespuestaAutenticacion respuesta =
                servicioAutenticacion.registrar(solicitud);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaAutenticacion> login(
            @Valid @RequestBody SolicitudLogin solicitud) {

        RespuestaAutenticacion respuesta =
                servicioAutenticacion.login(solicitud);

        return ResponseEntity.ok(respuesta);
    }
}
