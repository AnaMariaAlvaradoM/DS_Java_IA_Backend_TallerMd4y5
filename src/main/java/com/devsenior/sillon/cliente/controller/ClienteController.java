package com.devsenior.sillon.cliente.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.cliente.service.ClienteService;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo lectura: los clientes nacen al agendar. */
@Tag(name = "5 Clientes", description = "Clientes finales del salón (nacen al agendar)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return clienteService.listar(usuario.getSalonId());
    }
}
