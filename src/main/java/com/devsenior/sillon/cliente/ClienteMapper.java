package com.devsenior.sillon.cliente;

import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    public ClienteResponse convertir(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getTelefono());
    }
}
