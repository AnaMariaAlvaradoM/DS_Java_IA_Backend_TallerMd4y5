package com.devsenior.sillon.cliente.repositorio;

import com.devsenior.sillon.cliente.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findBySalonId(Long salonId);

    Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);
}