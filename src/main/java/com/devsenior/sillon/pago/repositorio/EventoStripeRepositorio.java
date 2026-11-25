
package com.devsenior.sillon.pago.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsenior.sillon.pago.modelo.EventoStripe;

public interface EventoStripeRepositorio
        extends JpaRepository<EventoStripe, String> {
}
