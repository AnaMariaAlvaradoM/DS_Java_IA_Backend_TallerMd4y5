package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findAllBySalonIdOrderByFechaCreacionDescIdDesc(Long salonId);

    Optional<SolicitudContacto> findByIdAndSalonId(Long id, Long salonId);
}
