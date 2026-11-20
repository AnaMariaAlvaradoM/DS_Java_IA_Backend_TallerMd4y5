package com.devsenior.sillon.cliente.dto;

import com.devsenior.sillon.cliente.modelo.Cliente;

public record ClienteResponse(
        Long id,
        String nombre,
        String telefono) {
    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono());
    }
}