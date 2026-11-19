package com.devsenior.sillon.cliente.service;

import com.devsenior.sillon.cliente.dto.ClienteResponse;
import com.devsenior.sillon.cliente.mapper.ClienteMapper;
import com.devsenior.sillon.cliente.model.Cliente;
import com.devsenior.sillon.cliente.repository.ClienteRepository;
import com.devsenior.sillon.salon.model.Salon;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    /** Busca al cliente por teléfono normalizado dentro del salón; si no existe, lo crea. Lo usa la agenda. */
    @Transactional
    public Cliente buscarOCrear(Salon salon, String nombre, String telefono) {
        String telefonoNormalizado = normalizarTelefono(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salon.getId(), telefonoNormalizado)
                .orElseGet(() -> {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(nombre.trim());
                    cliente.setTelefono(telefonoNormalizado);
                    cliente.setSalon(salon);
                    return clienteRepository.save(cliente);
                });
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long salonId) {
        return clienteRepository.findBySalonIdOrderByNombreAsc(salonId).stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    /** Deja solo dígitos y el signo +: "300 111 2233" → "3001112233". */
    public static String normalizarTelefono(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("[^0-9+]", "");
    }
}
