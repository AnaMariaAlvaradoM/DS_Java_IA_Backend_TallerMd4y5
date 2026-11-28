package com.devSenior.sillon.salon.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devSenior.sillon.salon.entidad.Salon;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {

	Optional<Salon> findBySlug(String slug);

	boolean existsBySlug(String slug);

	Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Salon s set s.creditosIa = s.creditosIa - 1 where s.id = :salonId and s.creditosIa > 0")
	int consumirCreditoSiHay(@Param("salonId") Long salonId);

}
