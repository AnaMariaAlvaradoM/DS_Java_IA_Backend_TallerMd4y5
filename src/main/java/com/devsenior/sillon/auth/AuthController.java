package com.devsenior.sillon.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServicio authServicio;

    public AuthController(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthRespuesta> registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        AuthRespuesta respuesta = authServicio.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRespuesta> login(@Valid @RequestBody LoginSolicitud solicitud) {
        return ResponseEntity.ok(authServicio.iniciarSesion(solicitud));
    }
}
