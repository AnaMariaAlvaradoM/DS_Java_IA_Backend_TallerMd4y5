package com.devsenior.sillon.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

    @Modifying
    @Query("UPDATE Salon s SET s.creditosIa = s.creditosIa + :cantidad WHERE s.id = :salonId")
    void sumarCreditos(@Param("salonId") Long salonId, @Param("cantidad") int cantidad);
}
