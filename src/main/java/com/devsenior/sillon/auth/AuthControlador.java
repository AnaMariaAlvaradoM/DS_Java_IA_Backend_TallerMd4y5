package com.devsenior.sillon.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {
    private final AuthServicio servicio;

    public AuthControlador(AuthServicio servicio) { this.servicio = servicio; }

    @PostMapping("/registro")
    public ResponseEntity<AuthRespuesta> registro(@Valid @RequestBody RegistroSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    @PostMapping("/login")
    public AuthRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) { return servicio.login(solicitud); }
}
