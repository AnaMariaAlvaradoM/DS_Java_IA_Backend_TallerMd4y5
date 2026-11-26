
package com.devsenior.sillon.ia.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.ia.modelo.SolicitudContacto;

import jakarta.persistence.LockModeType;

public interface SolicitudContactoRepositorio
        extends JpaRepository<SolicitudContacto, Long> {

    @Query("""
            SELECT s
            FROM SolicitudContacto s
            WHERE s.salon.id = :salonId
            ORDER BY s.fechaCreacion DESC, s.id DESC
            """)
    List<SolicitudContacto> buscarPorSalonOrdenadas(
            @Param("salonId") Long salonId);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SolicitudContacto s
            WHERE s.id = :solicitudId
              AND s.salon.id = :salonId
            """)
    Optional<SolicitudContacto> buscarPorIdYSalon(
            @Param("solicitudId") Long solicitudId,
            @Param("salonId") Long salonId);
}
