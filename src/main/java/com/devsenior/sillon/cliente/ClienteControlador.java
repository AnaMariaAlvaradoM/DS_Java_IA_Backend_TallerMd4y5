package com.devsenior.sillon.cliente;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteControlador {
    private final ClienteServicio servicio;

    public ClienteControlador(ClienteServicio servicio) { this.servicio = servicio; }

    @GetMapping
    public List<ClienteRespuesta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.listar(usuario.getSalonId());
    }
}
