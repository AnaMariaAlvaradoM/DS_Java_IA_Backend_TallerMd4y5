
package com.devsenior.sillon.salon;

import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Salon s WHERE s.id = :salonId")
    Optional<Salon> findByIdParaAgendar(
            @Param("salonId") Long salonId
    );

}
