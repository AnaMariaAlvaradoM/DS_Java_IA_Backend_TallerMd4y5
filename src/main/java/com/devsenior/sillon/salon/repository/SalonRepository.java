package com.devsenior.sillon.salon.repository;

import com.devsenior.sillon.salon.model.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSubscriptionId(String stripeSubscriptionId);


}
