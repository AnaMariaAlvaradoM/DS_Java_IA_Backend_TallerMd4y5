package com.devsenior.sillon.cliente;

import com.devsenior.sillon.cliente.dto.ClienteRespuesta;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final SalonRepository salonRepository;

    public ClienteService(ClienteRepository clienteRepository, SalonRepository salonRepository) {
        this.clienteRepository = clienteRepository;
        this.salonRepository = salonRepository;
    }

    public static String normalizarTelefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("[^0-9+]", "");
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String telefonoLimpio = normalizarTelefono(telefono);

        return clienteRepository.findBySalonIdAndTelefono(salonId, telefonoLimpio)
                .orElseGet(() -> {
                    Salon salon = salonRepository.findById(salonId)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
                    Cliente nuevo = new Cliente();
                    nuevo.setNombre(nombre.trim());
                    nuevo.setTelefono(telefonoLimpio);
                    nuevo.setSalon(salon);
                    return clienteRepository.save(nuevo);
                });
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listarClientes(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(c -> new ClienteRespuesta(c.getId(), c.getNombre(), c.getTelefono()))
                .toList();
    }
}
