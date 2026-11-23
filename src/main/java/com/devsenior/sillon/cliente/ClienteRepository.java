
package com.devsenior.sillon.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findBySalonId(Long salonId);

    Optional<Cliente> findBySalonIdAndTelefono(
            Long salonId,
            String telefono
    );
}
