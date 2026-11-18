package com.devsenior.sillon.cliente;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.dto.ClienteResponse;

@Service
public class ClienteService {

    private static final Pattern CARACTERES_NO_TELEFONICOS = Pattern.compile("[^0-9+]");

    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String normalizado = CARACTERES_NO_TELEFONICOS.matcher(telefono).replaceAll("");
        return clientes.findBySalonIdAndTelefono(salonId, normalizado).orElseGet(() -> {
            clientes.insertarSiNoExiste(salonId, nombre, normalizado);
            return clientes.findOneBySalonIdAndTelefono(salonId, normalizado)
                    .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el cliente tras el alta"));
        });
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long salonId) {
        return clientes.findAllBySalonId(salonId).stream()
                .map(cliente -> new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getTelefono()))
                .toList();
    }
}
