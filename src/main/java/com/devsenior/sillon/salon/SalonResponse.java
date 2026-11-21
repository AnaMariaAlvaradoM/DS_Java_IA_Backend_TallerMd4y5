package com.devsenior.sillon.salon;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record SalonResponse(Long id, String nombre, String slug, String ciudad,
                            String direccion, String telefono, Plan plan, Integer creditosIa,
                            ConfiguracionResponse configuracion, LocalDateTime fechaCreacion) {
    public record ConfiguracionResponse(LocalTime horaApertura, LocalTime horaCierre,
                                        boolean abreDomingos, Integer intervaloMinutos) {
    }
}
