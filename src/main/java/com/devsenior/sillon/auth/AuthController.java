package com.devsenior.sillon.auth;

import com.devsenior.sillon.auth.dto.AuthRespuesta;
import com.devsenior.sillon.auth.dto.LoginRequest;
import com.devsenior.sillon.auth.dto.RegistroRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthRespuesta> registro(@Valid @RequestBody RegistroRequest request) {
        AuthRespuesta respuesta = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRespuesta> login(@Valid @RequestBody LoginRequest request) {
        AuthRespuesta respuesta = authService.login(request);
        return ResponseEntity.ok(respuesta);
    }
}
