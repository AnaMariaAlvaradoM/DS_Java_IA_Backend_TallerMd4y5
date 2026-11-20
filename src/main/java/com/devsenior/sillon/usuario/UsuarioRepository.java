package com.devsenior.sillon.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findByIdAndSalonIdAndRol(Long id, Long salonId, Rol rol);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    @EntityGraph(attributePaths = "servicios")
    List<Usuario> findAllBySalonIdAndRol(Long salonId, Rol rol);
}
