package com.devsenior.sillon.cliente;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService clientes;
    public ClienteController(ClienteService clientes) { this.clientes = clientes; }

    @GetMapping
    public List<ClienteResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return clientes.listar(principal.getSalonId());
    }
}
