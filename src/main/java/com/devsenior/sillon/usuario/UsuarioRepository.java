package com.devsenior.sillon.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    List<Usuario> findBySalonIdAndRol(Long salonId, Rol rol);

    @EntityGraph(attributePaths = "salon")
    Optional<Usuario> findByEmail(String email);
}
