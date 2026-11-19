package com.devsenior.sillon.pago;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoStripeRepository extends JpaRepository<EventoStripe, String> {

    @Modifying
    @Query(value = """
            INSERT INTO eventos_stripe (id, tipo, fecha_procesado)
            VALUES (:id, :tipo, :fecha)
            """, nativeQuery = true)
    void registrar(@Param("id") String id, @Param("tipo") String tipo, @Param("fecha") LocalDateTime fecha);
}
