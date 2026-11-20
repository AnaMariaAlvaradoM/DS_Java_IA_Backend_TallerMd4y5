
package com.devsenior.sillon.salon;

import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Salon s WHERE s.id = :salonId")
    Optional<Salon> findByIdParaAgendar(
            @Param("salonId") Long salonId
    );


    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            UPDATE Salon s
            SET s.creditosIa = s.creditosIa - 1
            WHERE s.id = :salonId
            AND s.creditosIa > 0
            """)
    int consumirCreditoIa(
            @Param("salonId") Long salonId
    );
}
