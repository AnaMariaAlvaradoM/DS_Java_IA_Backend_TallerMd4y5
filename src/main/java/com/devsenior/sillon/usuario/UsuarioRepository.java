package com.devsenior.sillon.usuario;

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
}
