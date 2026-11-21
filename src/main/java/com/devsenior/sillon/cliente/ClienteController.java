package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteRespuesta;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteRespuesta>> listarClientes(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        List<ClienteRespuesta> clientes = clienteService.listarClientes(principal.getSalonId());
        return ResponseEntity.ok(clientes);
    }
}
