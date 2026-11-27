package com.devsenior.sillon.auth.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.auth.dto.RespuestaAuth;
import com.devsenior.sillon.auth.dto.SolicitudLogin;
import com.devsenior.sillon.auth.dto.SolicitudRegistro;
import com.devsenior.sillon.auth.servicio.AuthServicio;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1 Autenticación", description = "Registro de salones e inicio de sesión")
public class AuthControlador {

    private final AuthServicio authServicio;

    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaAuth> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authServicio.registrar(solicitud));
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaAuth> login(
            @RequestBody SolicitudLogin solicitud) {

        return ResponseEntity.ok(authServicio.login(solicitud));
    }

}