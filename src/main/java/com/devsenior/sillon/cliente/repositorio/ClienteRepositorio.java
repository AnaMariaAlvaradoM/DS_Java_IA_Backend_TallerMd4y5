package com.devsenior.sillon.cliente.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsenior.sillon.cliente.modelo.Cliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

	List<Cliente> findAllBySalonIdOrderByIdAsc(Long salonId);

	Optional<Cliente> findBySalonIdAndTelefono(Long salonId, String telefono);

}
