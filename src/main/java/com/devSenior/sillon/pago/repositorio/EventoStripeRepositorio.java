package com.devSenior.sillon.pago.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.pago.entidad.EventoStripe;

public interface EventoStripeRepositorio extends JpaRepository<EventoStripe, String> {
}
