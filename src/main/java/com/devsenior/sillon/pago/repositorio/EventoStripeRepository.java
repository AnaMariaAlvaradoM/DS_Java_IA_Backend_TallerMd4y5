package com.devsenior.sillon.pago.repositorio;

import com.devsenior.sillon.pago.modelo.EventoStripe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoStripeRepository extends JpaRepository<EventoStripe, String> {
}