package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    List<Cita> findBySalonIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long salonId, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
            select case when count(c) > 0 then true else false end
              from Cita c
             where c.salon.id = :salonId
               and c.estilista.id = :estilistaId
               and c.estado in :estados
               and c.inicio < :fin
               and c.fin > :inicio
            """)
    boolean hayCruce(@Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") List<EstadoCita> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("""
            select c from Cita c
             where c.salon.id = :salonId
               and c.estilista.id = :estilistaId
               and c.estado in :estados
               and c.inicio < :hasta
               and c.fin > :desde
            """)
    List<Cita> citasQueOcupan(@Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") List<EstadoCita> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
