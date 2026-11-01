package com.devsenior.sillon.servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByIdInAndSalonId(Collection<Long> ids, Long salonId);

    List<Servicio> findBySalonId(Long salonId);
}