package com.devSenior.sillon.cliente.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devSenior.sillon.cliente.entidad.Cliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findBySalon_IdAndTelefono(Long salonId, String telefono);

	List<Cliente> findBySalon_IdOrderByIdAsc(Long salonId);

	@Query("""
			select c from Cliente c
			where c.salon.id = :salonId
			and (lower(c.nombre) like lower(concat('%', :texto, '%'))
				or c.telefono like concat('%', :telefono, '%'))
			order by c.id
			""")
	List<Cliente> buscar(@Param("salonId") Long salonId, @Param("texto") String texto, @Param("telefono") String telefono);

}
