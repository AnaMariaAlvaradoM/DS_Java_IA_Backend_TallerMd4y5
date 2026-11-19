package com.devsenior.sillon.salon.repository;

import com.devsenior.sillon.salon.model.Salon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);
}
