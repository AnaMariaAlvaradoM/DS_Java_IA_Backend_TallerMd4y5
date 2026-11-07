package com.devsenior.sillon.cliente;

import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.SolicitudInvalidaException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServicio {
    private final ClienteRepositorio clientes;
    private final SalonRepositorio salones;

    public ClienteServicio(ClienteRepositorio clientes, SalonRepositorio salones) {
        this.clientes = clientes;
        this.salones = salones;
    }

    @Transactional
    public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
        String normalizado = normalizarTelefono(telefono);
        Salon salon = salones.buscarParaActualizar(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));
        return clientes.findBySalon_IdAndTelefono(salonId, normalizado).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setSalon(salon);
            cliente.setNombre(nombre);
            cliente.setTelefono(normalizado);
            return clientes.save(cliente);
        });
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listar(Long salonId) {
        return clientes.findAllBySalon_IdOrderByIdAsc(salonId).stream().map(ClienteRespuesta::desde).toList();
    }

    public String normalizarTelefono(String telefono) {
        if (telefono == null) { throw new SolicitudInvalidaException("El teléfono es obligatorio"); }
        String normalizado = telefono.replaceAll("[^0-9+]", "");
        if (normalizado.isEmpty()) { throw new SolicitudInvalidaException("El teléfono debe contener dígitos o +"); }
        return normalizado;
    }
}
