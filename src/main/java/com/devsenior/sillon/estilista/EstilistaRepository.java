package com.devsenior.sillon.estilista;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstilistaRepository extends JpaRepository<Estilista, Long> {

    List<Estilista> findBySalonId(Long salonId);

    Optional<Estilista> findByIdAndSalonId(Long id, Long salonId);
}
