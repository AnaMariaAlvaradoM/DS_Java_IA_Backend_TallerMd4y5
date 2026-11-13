package com.devsenior.sillon.servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findBySalonId(Long salonId);

    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);
}
