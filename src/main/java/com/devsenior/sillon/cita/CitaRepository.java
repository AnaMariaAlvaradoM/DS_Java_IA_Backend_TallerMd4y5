package com.devsenior.sillon.cita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.inicio >= :desde AND c.inicio < :hasta ORDER BY c.inicio ASC")
    List<Cita> findBySalonIdAndDia(
            @Param("salonId") Long salonId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.estilista.id = :estilistaId AND c.inicio >= :desde AND c.inicio < :hasta ORDER BY c.inicio ASC")
    List<Cita> findBySalonIdAndEstilistaIdAndDia(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.estilista.id = :estilistaId AND c.estado IN :estados AND c.inicio < :fin AND c.fin > :inicio")
    List<Cita> findCitasCruzadas(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") Collection<EstadoCita> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT c FROM Cita c WHERE c.salon.id = :salonId AND c.estilista.id = :estilistaId AND c.estado IN :estados AND c.inicio >= :desde AND c.inicio < :hasta")
    List<Cita> findCitasOcupadasEstilistaEnDia(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") Collection<EstadoCita> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
