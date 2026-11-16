package com.devSenior.sillon.salon.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devSenior.sillon.salon.entidad.Salon;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {

	Optional<Salon> findBySlug(String slug);

	boolean existsBySlug(String slug);

	Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

}
