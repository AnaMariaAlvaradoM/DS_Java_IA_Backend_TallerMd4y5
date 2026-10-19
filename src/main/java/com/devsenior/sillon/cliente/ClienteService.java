package com.devsenior.sillon.cliente;

import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clientes;
    private final SalonRepository salones;
    private final ClienteMapper mapper;

    public ClienteService(ClienteRepository clientes, SalonRepository salones, ClienteMapper mapper) {
        this.clientes = clientes;
        this.salones = salones;
        this.mapper = mapper;
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String normalizado = telefono.replaceAll("[^0-9+]", "");
        return clientes.findBySalonIdAndTelefono(salonId, normalizado).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setNombre(nombre);
            cliente.setTelefono(normalizado);
            cliente.setSalon(salones.findById(salonId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado")));
            return clientes.saveAndFlush(cliente);
        });
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long salonId) {
        return clientes.findAllBySalonId(salonId).stream().map(mapper::convertir).toList();
    }
}
