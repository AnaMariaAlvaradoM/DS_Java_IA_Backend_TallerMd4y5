package com.devsenior.sillon.salon;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Salon s where s.stripeSuscripcionId = :suscripcionId")
    Optional<Salon> buscarPorSuscripcionParaActualizar(@Param("suscripcionId") String suscripcionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Salon s where s.id = :id")
    Optional<Salon> buscarParaActualizar(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :id and s.creditosIa > 0")
    int consumirCreditoIa(@Param("id") Long id);

    @Query("select s.creditosIa from Salon s where s.id = :id")
    Optional<Integer> consultarCreditosIa(@Param("id") Long id);
}
