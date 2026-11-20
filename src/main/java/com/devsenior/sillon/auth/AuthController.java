package com.devsenior.sillon.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.auth.dto.RespuestaAuth;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaAuth> registrar(@Valid @RequestBody RegistroRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(peticion));
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaAuth> login(@Valid @RequestBody LoginRequest peticion) {
        return ResponseEntity.ok(authService.login(peticion));
    }
}
