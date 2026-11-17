package com.devSenior.sillon.cliente.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.cliente.entidad.Cliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findBySalon_IdAndTelefono(Long salonId, String telefono);

	List<Cliente> findBySalon_IdOrderByIdAsc(Long salonId);

}
