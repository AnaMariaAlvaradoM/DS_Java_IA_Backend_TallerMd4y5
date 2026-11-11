package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteResponseDto;
import com.devsenior.sillon.salon.Salon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente buscarOCrear(String nombre, String telefono, Salon salon) {
        String telefonoLimpio = sanitizarTelefono(telefono);

        return clienteRepository.findBySalonIdAndTelefono(salon.getId(), telefonoLimpio)
                .map(clienteExistente -> {
                    // Si cambia el nombre entregado en la reserva, se puede actualizar opcionalmente
                    if (nombre != null && !nombre.isBlank() && !nombre.equals(clienteExistente.getNombre())) {
                        clienteExistente.setNombre(nombre.trim());
                    }
                    return clienteExistente;
                })
                .orElseGet(() -> clienteRepository.save(
                        new Cliente(nombre.trim(), telefonoLimpio, salon)
                ));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDto> listarClientes(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(c -> new ClienteResponseDto(c.getId(), c.getNombre(), c.getTelefono()))
                .toList();
    }

    private String sanitizarTelefono(String telefono) {
        if (telefono == null) return "";
        return telefono.replaceAll("[^0-9+]", "");
    }
}