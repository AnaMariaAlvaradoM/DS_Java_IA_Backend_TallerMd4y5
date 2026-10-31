package com.devsenior.sillon.estilista;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_estilista")
public class HorarioEstilista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estilista_id", nullable = false)
    private Estilista estilista;

    public HorarioEstilista() {}

    public HorarioEstilista(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, Estilista estilista) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estilista = estilista;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DiaSemana getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DiaSemana diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public Estilista getEstilista() { return estilista; }
    public void setEstilista(Estilista estilista) { this.estilista = estilista; }
}
