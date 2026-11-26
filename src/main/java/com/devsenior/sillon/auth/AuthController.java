package com.devsenior.sillon.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthRespuesta registro(@Valid @RequestBody RegistroPeticion peticion) {
        return authService.registrar(peticion);
    }

    @PostMapping("/login")
    public AuthRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
        return authService.login(peticion);
    }
}
