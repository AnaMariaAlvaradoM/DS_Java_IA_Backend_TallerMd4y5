package com.devsenior.sillon.pago;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoStripeRepository extends JpaRepository<EventoStripe, Long> {

    boolean existsByStripeEventId(String stripeEventId);
}
