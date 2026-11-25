package com.devsenior.sillon.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Lo usa el webhook de Stripe (Fase 6) para ubicar el salon cuando se cancela su suscripcion. */
    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

    /** Descuenta 1 crédito de IA de forma atómica sólo si el salón tiene créditos disponibles. */
    @Modifying
    @Query("UPDATE Salon s SET s.creditosIa = s.creditosIa - 1 WHERE s.id = :salonId AND s.creditosIa > 0")
    int descontarCredito(@Param("salonId") Long salonId);
}
