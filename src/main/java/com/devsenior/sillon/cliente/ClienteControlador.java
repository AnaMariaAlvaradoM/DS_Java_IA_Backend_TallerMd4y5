package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

/**
 * Consulta de clientes del salon (solo DUENO).
 * Los clientes se crean automaticamente al agendar citas.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteControlador {

    private final ClienteServicio clienteServicio;

    public ClienteControlador(ClienteServicio clienteServicio) {
        this.clienteServicio = clienteServicio;
    }

    @GetMapping
    public List<ClienteRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return clienteServicio.listar(principal.getSalonId());
    }
}
