package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@Service
public class ClienteServicio {

    private final ClienteRepository clienteRepository;
    private final SalonRepository salonRepository;

    public ClienteServicio(ClienteRepository clienteRepository, SalonRepository salonRepository) {
        this.clienteRepository = clienteRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String normalizado = normalizarTelefono(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salonId, normalizado)
                .orElseGet(() -> crear(salonId, nombre, normalizado));
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listar(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(cliente -> new ClienteRespuesta(
                        cliente.getId(),
                        cliente.getNombre(),
                        cliente.getTelefono()))
                .toList();
    }

    public String normalizarTelefono(String telefono) {
        return telefono.replaceAll("[^0-9+]", "");
    }

    private Cliente crear(Long salonId, String nombre, String telefono) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        Cliente cliente = new Cliente();
        cliente.setSalon(salon);
        cliente.setNombre(nombre.trim());
        cliente.setTelefono(telefono);
        return clienteRepository.save(cliente);
    }
}
