package com.devsenior.sillon.usuario;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    long countBySalon_IdAndRol(Long salonId, Rol rol);
    List<Usuario> findAllBySalon_IdAndRolOrderByIdAsc(Long salonId, Rol rol);
}
