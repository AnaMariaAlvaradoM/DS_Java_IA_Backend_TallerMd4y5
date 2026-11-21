package com.devsenior.sillon.ia;

import com.devsenior.sillon.salon.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditoIaRepository extends JpaRepository<Salon, Long> {
    @Modifying(clearAutomatically = true)
    @Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :salonId and s.creditosIa > 0")
    int consumirSiDisponible(@Param("salonId") Long salonId);
}
