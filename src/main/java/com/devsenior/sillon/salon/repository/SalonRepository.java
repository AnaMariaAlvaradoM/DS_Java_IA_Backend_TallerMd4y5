package com.devsenior.sillon.salon.repository;

import com.devsenior.sillon.salon.model.Salon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

    /**
     * Resta un crédito en una sola sentencia y solo si quedan créditos.
     * Dos peticiones al mismo tiempo nunca dejan los créditos en negativo. Devuelve las filas afectadas (0 o 1).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :salonId and s.creditosIa > 0")
    int consumirCredito(@Param("salonId") Long salonId);
}
