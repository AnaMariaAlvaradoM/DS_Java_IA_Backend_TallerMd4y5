package com.devsenior.sillon.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Rutas publicas: no piden token. El controlador solo recibe, valida y delega en el servicio. */
@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private final AuthServicio authServicio;

    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthRespuesta registrar(@Valid @RequestBody RegistroPeticion peticion) {
        return authServicio.registrar(peticion);
    }

    @PostMapping("/login")
    public AuthRespuesta iniciarSesion(@Valid @RequestBody LoginPeticion peticion) {
        return authServicio.iniciarSesion(peticion);
    }
}
