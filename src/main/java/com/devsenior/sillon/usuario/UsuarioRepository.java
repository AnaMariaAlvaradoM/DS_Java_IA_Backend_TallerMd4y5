package com.devsenior.sillon.usuario;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Usuario> findByIdAndSalonId(Long id, Long salonId);
    long countBySalonIdAndRol(Long salonId, Rol rol);
    List<Usuario> findAllBySalonIdAndRol(Long salonId, Rol rol);
    List<Usuario> findAllBySalonIdAndRolAndActivoTrue(Long salonId, Rol rol);
    Optional<Usuario> findByIdAndSalonIdAndRol(Long id, Long salonId, Rol rol);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.salon.id = :salonId and u.rol = :rol")
    Optional<Usuario> findParaAgendar(@Param("id") Long id, @Param("salonId") Long salonId, @Param("rol") Rol rol);
}
