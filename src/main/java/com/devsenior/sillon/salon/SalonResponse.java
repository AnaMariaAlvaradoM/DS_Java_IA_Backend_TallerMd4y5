package com.devsenior.sillon.salon;

import java.time.LocalDateTime;

public record SalonResponse(Long id, String nombre, String slug, String ciudad, String direccion,
                            String telefono, Plan plan, int creditosIa,
                            ConfiguracionSalonResponse configuracion, LocalDateTime fechaCreacion) {
}
