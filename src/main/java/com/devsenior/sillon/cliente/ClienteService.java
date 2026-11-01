package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteRequest;
import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.cliente.mapper.ClienteMapper;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final SalonRepository salonRepository;

    public ClienteService(ClienteRepository clienteRepository, SalonRepository salonRepository) {
        this.clienteRepository = clienteRepository;
        this.salonRepository = salonRepository;
    }

    public ClienteResponse crearCliente(ClienteRequest request, Long salonId) {
    Salon salon = salonRepository.findById(salonId).orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setSalon(salon);
        clienteRepository.save(cliente);
        return ClienteMapper.toClienteResponse(cliente);
    }
    public List<ClienteResponse> listarClientesSalon(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(ClienteMapper::toClienteResponse)
                .collect(Collectors.toList());
    }

}
