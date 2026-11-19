package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findAllBySalonIdOrderByFechaCreacionDescIdDesc(Long salonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudContacto> findOneByIdAndSalonId(Long id, Long salonId);
}
