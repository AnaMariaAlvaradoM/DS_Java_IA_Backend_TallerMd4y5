
package com.devsenior.sillon.salon.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.modelo.Salon;

import jakarta.persistence.LockModeType;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {

        Optional<Salon> findBySlug(String slug);

        boolean existsBySlug(String slug);

        Optional<Salon> findByStripeSuscripcionId(
                        String stripeSuscripcionId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM Salon s WHERE s.id = :salonId")
        Optional<Salon> buscarBloqueadoPorId(
                        @Param("salonId") Long salonId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT s FROM Salon s
                        WHERE s.stripeSuscripcionId = :suscripcionId
                        """)
        Optional<Salon> buscarBloqueadoPorSuscripcionId(
                        @Param("suscripcionId") String suscripcionId);

        @Modifying
        @Transactional
        @Query("""
                        UPDATE Salon s
                        SET s.creditosIa = s.creditosIa - 1
                        WHERE s.id = :salonId
                          AND s.creditosIa > 0
                        """)
        int descontarCreditoIa(@Param("salonId") Long salonId);
}
