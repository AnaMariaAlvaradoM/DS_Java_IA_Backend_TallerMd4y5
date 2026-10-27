package com.devsenior.sillon.chat;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SolicitudRepository extends JpaRepository<SolicitudContacto, Long> {
    List<SolicitudContacto> findAllBySalonIdOrderByFechaCreacionDescIdDesc(Long salonId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudContacto> findByIdAndSalonId(Long id, Long salonId);
}
