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
    Optional<Salon> findBySuscripcionStripeId(String suscripcionStripeId);
    @Query("SELECT s.creditos FROM Salon s WHERE s.id = :salonId")
    Integer obtenerCreditos(@Param("salonId") Long salonId);

    @Modifying
    @Query("UPDATE Salon s SET s.creditos = s.creditos - 1 WHERE s.id = :salonId AND s.creditos > 0")
    int consumirUnCredito(@Param("salonId") Long salonId);
}
