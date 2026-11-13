package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteRequest;
import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('DUENO')")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> getAllClientes(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal){
        List<ClienteResponse> clientes = clienteService.listarClientesSalon(usuarioPrincipal.getSalonId());
        return ResponseEntity.ok(clientes);
    }
    @PostMapping("/crear")
    public ResponseEntity<ClienteResponse> crearCliente(@AuthenticationPrincipal UsuarioPrincipal usuarioPrincipal,@Valid ClienteRequest request){
        ClienteResponse clienteResponse = clienteService.crearCliente(request, usuarioPrincipal.getSalonId());
        return ResponseEntity.ok(clienteResponse);
    }
}
