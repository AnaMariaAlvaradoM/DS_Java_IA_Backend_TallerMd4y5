package com.devsenior.sillon.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Lo usa el webhook de Stripe (Fase 6) para ubicar el salon cuando se cancela su suscripcion. */
    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);
}
