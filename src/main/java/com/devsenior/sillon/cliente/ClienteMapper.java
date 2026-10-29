package com.devsenior.sillon.cliente;

public class ClienteMapper {

    private ClienteMapper() {
    }

    public static ClienteRespuesta aRespuesta(Cliente cliente) {
        return new ClienteRespuesta(cliente.getId(), cliente.getNombre(), cliente.getTelefono());
    }
}
