package com.devsenior.sillon.cliente.servicio;

import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.cliente.repositorio.ClienteRepository;
import com.devsenior.sillon.salon.modelo.Salon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarPorSalon(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(ClienteResponse::desde)
                .toList();
    }

    @Transactional
    public Cliente buscarOCrear(Salon salon, String nombre, String telefono) {
        String telefonoNormalizado = normalizarTelefono(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salon.getId(), telefonoNormalizado)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setSalon(salon);
                    nuevo.setNombre(nombre.trim());
                    nuevo.setTelefono(telefonoNormalizado);
                    return clienteRepository.save(nuevo);
                });
    }

    public static String normalizarTelefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("[^0-9+]", "");
    }
}