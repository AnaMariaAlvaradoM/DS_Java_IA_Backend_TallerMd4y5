package com.devsenior.sillon.estilista;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioEstilistaRepository extends JpaRepository<HorarioEstilista, Long> {

    List<HorarioEstilista> findByEstilistaId(Long estilistaId);

    void deleteByEstilistaId(Long estilistaId);
}