package com.devSenior.sillon.usuario.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

	boolean existsByEmail(String email);

	@EntityGraph(attributePaths = "salon")
	Optional<Usuario> findByEmail(String email);

	@Override
	@EntityGraph(attributePaths = "servicios")
	Optional<Usuario> findById(Long id);

	long countBySalon_IdAndRol(Long salonId, Rol rol);

	@EntityGraph(attributePaths = "servicios")
	List<Usuario> findBySalon_IdOrderByIdAsc(Long salonId);

	@EntityGraph(attributePaths = "servicios")
	List<Usuario> findBySalon_IdAndRolOrderByIdAsc(Long salonId, Rol rol);

	@EntityGraph(attributePaths = "servicios")
	Optional<Usuario> findByIdAndSalon_Id(Long id, Long salonId);

}
