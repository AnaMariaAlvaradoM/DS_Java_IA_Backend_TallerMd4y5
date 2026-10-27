package com.devsenior.sillon.salon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Salon s where s.id = :id")
    Optional<Salon> findByIdParaActualizar(@Param("id") Long id);

    @Query("select s.creditosIa from Salon s where s.id = :id")
    Optional<Integer> consultarCreditos(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :id and s.creditosIa > 0")
    int consumirCredito(@Param("id") Long id);
}
