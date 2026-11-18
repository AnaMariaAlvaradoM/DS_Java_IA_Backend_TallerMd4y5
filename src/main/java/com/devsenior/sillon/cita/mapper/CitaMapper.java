package com.devsenior.sillon.cita.mapper;

import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.model.Cita;

import java.time.LocalDateTime;

public class CitaMapper {
    public static CitaResponse toResponse(Cita cita) {
        LocalDateTime inicio = cita.getFechaHora();
        LocalDateTime finCalculado = inicio.plusMinutes(cita.getServicio().getDuracionMinutos());
        return new CitaResponse(
                cita.getId(),
                cita.getFechaHora(),
                finCalculado,
                cita.getEstadoCita(),
                cita.getOrigen(),
                cita.getPrecio(),
                cita.getServicio().getId(),
                cita.getServicio().getNombre(),
                cita.getEstilista().getId(),
                cita.getEstilista().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                cita.getNota(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion()
        );
    }
}
