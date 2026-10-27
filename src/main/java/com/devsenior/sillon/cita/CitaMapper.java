package com.devsenior.sillon.cita;

import org.springframework.stereotype.Component;

@Component
public class CitaMapper {
    public CitaResponse convertir(Cita cita) {
        return new CitaResponse(cita.getId(), cita.getInicio(), cita.getFin(), cita.getEstado(), cita.getOrigen(),
                cita.getPrecio(), cita.getServicio().getId(), cita.getServicio().getNombre(), cita.getEstilista().getId(),
                cita.getEstilista().getNombre(), cita.getCliente().getNombre(), cita.getCliente().getTelefono(),
                cita.getNota(), cita.getFechaCreacion(), cita.getFechaActualizacion());
    }
}
