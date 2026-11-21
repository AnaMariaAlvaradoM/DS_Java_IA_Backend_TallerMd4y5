package com.devsenior.sillon.cliente;

import com.devsenior.sillon.salon.Salon;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long salonId) {
        return clientes.findBySalonIdOrderByNombreAsc(salonId).stream()
                .map(cliente -> new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getTelefono()))
                .toList();
    }

    @Transactional
    public Cliente buscarOCrear(Salon salon, String nombre, String telefono) {
        String normalizado = normalizarTelefono(telefono);
        return clientes.findBySalonIdAndTelefono(salon.getId(), normalizado)
                .orElseGet(() -> {
                    Cliente cliente = new Cliente();
                    cliente.setSalon(salon);
                    cliente.setNombre(nombre.trim());
                    cliente.setTelefono(normalizado);
                    return clientes.save(cliente);
                });
    }

    public String normalizarTelefono(String telefono) {
        return telefono.replaceAll("[^0-9+]", "");
    }

    public record ClienteResponse(Long id, String nombre, String telefono) { }
}
