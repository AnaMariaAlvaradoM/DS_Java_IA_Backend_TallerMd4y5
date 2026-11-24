package com.devsenior.sillon.usuario.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countBySalonIdAndRol(Long salonId, Rol rol);

    @Query("""
            SELECT DISTINCT u
            FROM Usuario u
            LEFT JOIN FETCH u.servicios
            WHERE u.salon.id = :salonId
              AND u.rol = :rol
            ORDER BY u.id ASC
            """)
    List<Usuario> buscarEstilistasConServicios(
            @Param("salonId") Long salonId,
            @Param("rol") Rol rol);

    @Query("""
            SELECT DISTINCT u
            FROM Usuario u
            LEFT JOIN FETCH u.servicios
            WHERE u.id = :id
              AND u.salon.id = :salonId
              AND u.rol = :rol
            """)
    Optional<Usuario> buscarEstilistaConServiciosPorId(
            @Param("id") Long id,
            @Param("salonId") Long salonId,
            @Param("rol") Rol rol);

}