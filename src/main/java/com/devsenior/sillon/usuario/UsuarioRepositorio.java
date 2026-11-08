package com.devsenior.sillon.usuario;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    long countBySalon_IdAndRol(Long salonId, Rol rol);
    List<Usuario> findAllBySalon_IdAndRolOrderByIdAsc(Long salonId, Rol rol);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.salon.id = :salonId and u.rol = :rol")
    Optional<Usuario> buscarEstilistaParaAgendar(@Param("id") Long id,
            @Param("salonId") Long salonId, @Param("rol") Rol rol);
}
