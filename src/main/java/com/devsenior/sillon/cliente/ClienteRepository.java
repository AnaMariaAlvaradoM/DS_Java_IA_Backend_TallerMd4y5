package com.devsenior.sillon.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);

    List<Cliente> findBySalonId(Long salonId);
}
