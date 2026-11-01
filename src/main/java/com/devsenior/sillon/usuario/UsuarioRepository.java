package com.devsenior.sillon.usuario;

import com.devsenior.sillon.usuario.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    List<Usuario> findBySalonIdAndRol(Long salonId, Rol rol);

    Optional<Usuario> findByIdAndSalonIdAndRol(Long id, Long salonId, Rol rol);
}
