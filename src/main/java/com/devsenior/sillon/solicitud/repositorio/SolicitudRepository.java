package com.devsenior.sillon.solicitud.repositorio;

import com.devsenior.sillon.solicitud.modelo.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findBySalonIdOrderByFechaCreacionDesc(Long salonId);

    Optional<Solicitud> findByIdAndSalonId(Long id, Long salonId);
}