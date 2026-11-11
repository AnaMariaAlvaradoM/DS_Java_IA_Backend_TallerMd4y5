package com.devsenior.sillon.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);
    List<Cliente> findBySalonId(Long salonId);
}