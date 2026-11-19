package com.devsenior.sillon.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Salon> findOneById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Salon> findOneByStripeSuscripcionId(String stripeSuscripcionId);
}
