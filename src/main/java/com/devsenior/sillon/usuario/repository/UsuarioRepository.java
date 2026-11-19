package com.devsenior.sillon.usuario.repository;

import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    List<Usuario> findBySalonIdAndRolOrderByNombreAsc(Long salonId, Rol rol);

    List<Usuario> findBySalonIdAndRolAndActivoTrueOrderByNombreAsc(Long salonId, Rol rol);

    Optional<Usuario> findByIdAndSalonIdAndRol(Long id, Long salonId, Rol rol);
}
