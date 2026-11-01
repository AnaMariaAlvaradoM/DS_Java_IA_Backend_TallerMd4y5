package com.devsenior.sillon.usuario.repository;

import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
    List<Usuario> findBySalonIdAndRol(Long salonId, Rol rol);
    long countBySalonIdAndRol(Long salonId, Rol rol);
    Optional<Usuario> findByIdAndSalonIdAndRol(long id, Long salonId, Rol rol);
    List<Usuario> findBySalonIdAndRolAndActivoTrue(Long salonId, Rol rol);

    Optional<Usuario> findByIdAndSalonIdAndActivoTrueAndRol(
            Long id,
            Long salonId,
            Rol rol
    );
}
