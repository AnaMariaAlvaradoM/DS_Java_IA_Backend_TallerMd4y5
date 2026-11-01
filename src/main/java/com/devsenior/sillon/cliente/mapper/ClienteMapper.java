package com.devsenior.sillon.cliente.mapper;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.dto.ClienteResponse;

public class ClienteMapper {
    public static ClienteResponse toClienteResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono()
        );
    }
}
