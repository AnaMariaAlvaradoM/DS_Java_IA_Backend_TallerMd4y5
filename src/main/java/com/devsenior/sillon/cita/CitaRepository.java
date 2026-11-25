package com.devsenior.sillon.cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    @EntityGraph(attributePaths = {"servicio", "estilista", "cliente", "salon"})
    Optional<Cita> findByIdAndSalonId(Long id, Long salonId);

    /**
     * Busca cruces de citas para un estilista en un salon:
     * Citas en estados que ocupan agenda (PENDIENTE, CONFIRMADA) donde:
     * c.inicio < :fin AND c.fin > :inicio
     */
    @Query("""
        SELECT c FROM Cita c
        WHERE c.salon.id = :salonId
          AND c.estilista.id = :estilistaId
          AND c.estado IN (:estados)
          AND c.inicio < :fin
          AND c.fin > :inicio
    """)
    List<Cita> findCruce(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") Collection<EstadoCita> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    /** Citas del salon en un dia, ordenadas por inicio ascendente (para el dueno). */
    @EntityGraph(attributePaths = {"servicio", "estilista", "cliente"})
    List<Cita> findBySalonIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /** Citas del estilista en un dia, ordenadas por inicio ascendente (para el estilista). */
    @EntityGraph(attributePaths = {"servicio", "estilista", "cliente"})
    List<Cita> findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
            Long salonId,
            Long estilistaId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /** Citas ocupadas del estilista en una fecha para calcular disponibilidad (Fase 5). */
    @Query("""
        SELECT c FROM Cita c
        WHERE c.salon.id = :salonId
          AND c.estilista.id = :estilistaId
          AND c.estado IN (:estados)
          AND c.inicio >= :desde
          AND c.inicio < :hasta
    """)
    List<Cita> findOcupadasPorEstilistaYFecha(
            @Param("salonId") Long salonId,
            @Param("estilistaId") Long estilistaId,
            @Param("estados") Collection<EstadoCita> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
