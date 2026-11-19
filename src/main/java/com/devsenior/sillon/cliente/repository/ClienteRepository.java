package com.devsenior.sillon.cliente.repository;

import com.devsenior.sillon.cliente.model.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);

    List<Cliente> findBySalonIdOrderByNombreAsc(Long salonId);
}
