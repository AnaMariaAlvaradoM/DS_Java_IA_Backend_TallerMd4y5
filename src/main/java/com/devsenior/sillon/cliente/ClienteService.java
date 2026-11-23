
package com.devsenior.sillon.cliente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.devsenior.sillon.salon.Salon;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarPorSalon(Long salonId) {
        return clienteRepository.findBySalonId(salonId);
    }

    @Transactional
    public Cliente buscarOCrear(
            String nombre,
            String telefono,
            Salon salon
    ) {
        if (salon == null || salon.getId() == null) {
            throw new IllegalArgumentException("Salón inválido");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del cliente es obligatorio"
            );
        }

        String telefonoNormalizado = TelefonoUtils.normalizar(telefono);

        if (telefonoNormalizado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El teléfono del cliente es obligatorio"
            );
        }

        return clienteRepository
                .findBySalonIdAndTelefono(
                        salon.getId(),
                        telefonoNormalizado
                )
                .orElseGet(() -> clienteRepository.save(
                        new Cliente(
                                nombre.trim(),
                                telefonoNormalizado,
                                salon
                        )
                ));
    }
}
