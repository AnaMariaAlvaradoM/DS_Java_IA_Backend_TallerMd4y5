package com.devsenior.sillon.salon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Salon> findOneById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Salon> findOneByStripeSuscripcionId(String stripeSuscripcionId);

    @Modifying(clearAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :salonId and s.creditosIa > 0")
    int consumirCredito(@Param("salonId") Long salonId);
}
