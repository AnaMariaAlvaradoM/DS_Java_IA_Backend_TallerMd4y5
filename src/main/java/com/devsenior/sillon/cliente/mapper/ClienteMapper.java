package com.devsenior.sillon.cliente.mapper;

import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.cliente.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getTelefono());
    }
}
