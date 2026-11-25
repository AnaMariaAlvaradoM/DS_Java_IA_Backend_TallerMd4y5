package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
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

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listar(Long salonId) {
        return clienteRepository.findBySalonId(salonId).stream()
                .map(c -> new ClienteRespuesta(c.getId(), c.getNombre(), c.getTelefono()))
                .toList();
    }

    /**
     * Busca el cliente por telefono normalizado dentro del salon; si no existe, lo crea.
     * El telefono se guarda solo con digitos y el signo +.
     */
    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String telefonoNormalizado = NormalizadorTelefono.normalizar(telefono);
        return clienteRepository.findBySalonIdAndTelefono(salonId, telefonoNormalizado)
                .orElseGet(() -> {
                    Salon salon = salonRepository.findById(salonId)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
                    Cliente nuevo = new Cliente();
                    nuevo.setNombre(nombre != null ? nombre.trim() : "");
                    nuevo.setTelefono(telefonoNormalizado);
                    nuevo.setSalon(salon);
                    return clienteRepository.save(nuevo);
                });
    }
}
