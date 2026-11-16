package com.devSenior.sillon.usuario.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.usuario.entidad.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

	boolean existsByEmail(String email);

	@EntityGraph(attributePaths = "salon")
	Optional<Usuario> findByEmail(String email);

}
