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

    /** Resta 1 crédito solo si quedan. Devuelve cuántas filas cambió (0 = no había créditos). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :id and s.creditosIa > 0")
    int descontarCredito(@Param("id") Long id);
}
