package com.devsenior.sillon.servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<ServicioSalon, Long> {

    List<ServicioSalon> findBySalonIdAndActivoTrue(Long salonId);

    List<ServicioSalon> findBySalonId(Long salonId);

    Optional<ServicioSalon> findByIdAndSalonId(Long id, Long salonId);
}