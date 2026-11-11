package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteResponseDto;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<List<ClienteResponseDto>> listarClientes(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(clienteService.listarClientes(usuario.getSalonId()));
    }
}