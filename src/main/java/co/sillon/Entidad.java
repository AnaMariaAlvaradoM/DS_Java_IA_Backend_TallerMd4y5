package co.sillon;

import jakarta.persistence.*;

@MappedSuperclass
abstract class Entidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id;
}
