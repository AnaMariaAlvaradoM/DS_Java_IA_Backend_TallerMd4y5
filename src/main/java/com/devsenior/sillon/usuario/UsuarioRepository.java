package com.devsenior.sillon.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Trae el salon en la misma consulta (join). El filtro de seguridad corre fuera de una transaccion y
     * necesita el salonId; con el salon cargado no hay riesgo de "could not initialize proxy".
     */
    @EntityGraph(attributePaths = "salon")
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "servicios")
    List<Usuario> findBySalonIdAndRol(Long salonId, Rol rol);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    @EntityGraph(attributePaths = "servicios")
    Optional<Usuario> findByIdAndSalonIdAndRol(Long id, Long salonId, Rol rol);
}

