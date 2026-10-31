package com.devsenior.sillon.servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findBySalonIdAndActivoTrue(Long salonId);

    List<Servicio> findBySalonId(Long salonId);

    Optional<Servicio> findByIdAndSalonId(Long id, Long salonId);
}