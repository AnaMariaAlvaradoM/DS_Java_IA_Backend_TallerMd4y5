package com.devsenior.sillon.salon.repositorio;

import com.devsenior.sillon.salon.modelo.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Modifying
    @Query("UPDATE Salon s SET s.creditosIa = s.creditosIa - 1 WHERE s.id = :id AND s.creditosIa > 0")
    int descontarCreditoAtomico(@Param("id") Long id);
}