package com.devsenior.sillon.cliente;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findBySalon_IdAndTelefono(Long salonId, String telefono);
    List<Cliente> findAllBySalon_IdOrderByIdAsc(Long salonId);
}
