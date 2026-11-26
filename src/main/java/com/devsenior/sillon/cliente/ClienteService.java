package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.Salon;

/** Los clientes no se crean a mano: nacen al agendar ("buscar o crear" por teléfono). */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente buscarOCrear(Salon salon, String nombre, String telefono) {
        String telefonoNormalizado = normalizarTelefono(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salon.getId(), telefonoNormalizado)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setNombre(nombre.trim());
                    nuevo.setTelefono(telefonoNormalizado);
                    nuevo.setSalon(salon);
                    return clienteRepository.save(nuevo);
                });
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listar(Long salonId) {
        return clienteRepository.findAllBySalonIdOrderByIdAsc(salonId).stream()
                .map(c -> new ClienteRespuesta(c.getId(), c.getNombre(), c.getTelefono()))
                .toList();
    }

    /** Solo dígitos y el signo +. "300 111 2233" → "3001112233". */
    public static String normalizarTelefono(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("[^0-9+]", "");
    }
}
