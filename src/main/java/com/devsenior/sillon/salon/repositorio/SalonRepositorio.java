package com.devsenior.sillon.salon.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsenior.sillon.salon.modelo.Salon;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {

	Optional<Salon> findBySlug(String slug);

	boolean existsBySlug(String slug);

	Optional<Salon> findByStripeSuscripcionId(String stripeSuscripcionId);

}
