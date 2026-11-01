package com.devsenior.sillon.salon.repositorio;

import com.devsenior.sillon.salon.modelo.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSubscriptionId(String stripeSubscriptionId);
}