package com.devsenior.sillon.cliente;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);
    List<Cliente> findAllBySalonId(Long salonId);
}
