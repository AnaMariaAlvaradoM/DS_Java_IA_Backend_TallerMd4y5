package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteServicio clienteServicio;

    public ClienteController(ClienteServicio clienteServicio) {
        this.clienteServicio = clienteServicio;
    }

    @GetMapping
    public ResponseEntity<List<ClienteRespuesta>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(clienteServicio.listar(principal.getSalonId()));
    }
}
