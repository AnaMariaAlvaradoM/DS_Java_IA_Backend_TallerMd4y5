package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final SalonRepository salonRepository;

    public ClienteService(ClienteRepository clienteRepository, SalonRepository salonRepository) {
        this.clienteRepository = clienteRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String telefonoNormalizado = normalizarTelefono(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salonId, telefonoNormalizado)
                .orElseGet(() -> {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(nombre);
                    cliente.setTelefono(telefonoNormalizado);
                    cliente.setSalon(salonRepository.getReferenceById(salonId));
                    return clienteRepository.save(cliente);
                });
    }

    public List<ClienteRespuesta> listarPorSalon(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(ClienteMapper::aRespuesta)
                .toList();
    }

    private String normalizarTelefono(String telefono) {
        return telefono.replaceAll("[^0-9+]", "");
    }
}
