package com.devsenior.sillon.salon.repository;

import com.devsenior.sillon.salon.model.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSubscriptionId(String stripeSubscriptionId);
    @Query("SELECT s.creditosIa FROM Salon s WHERE s.id = :salonId")
    Integer obtenerCreditos(@Param("salonId") Long salonId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Salon s SET s.creditosIa = s.creditosIa - 1 WHERE s.id = :salonId AND s.creditosIa > 0")
    int descontarCredito(@Param("salonId") Long salonId);

    @Query("SELECT s.creditosIa FROM Salon s WHERE s.id = :salonId")
    Integer obtenerCreditosActuales(@Param("salonId") Long salonId);
}
