
package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar(
            @AuthenticationPrincipal(expression = "salonId")
            Long salonId
    ) {
        return clienteService.listarPorSalon(salonId)
                .stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }
}
