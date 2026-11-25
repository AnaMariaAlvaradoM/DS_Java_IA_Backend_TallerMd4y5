package com.devsenior.sillon.cita;

import org.springframework.stereotype.Component;

/** Mapper entre la entidad Cita y su DTO CitaRespuesta. */
@Component
public class CitaMapper {

    public CitaRespuesta aRespuesta(Cita cita) {
        return new CitaRespuesta(
                cita.getId(),
                cita.getInicio(),
                cita.getFin(),
                cita.getEstado(),
                cita.getOrigen(),
                cita.getPrecio() != null ? cita.getPrecio() : 0,
                cita.getServicio().getId(),
                cita.getServicio().getNombre(),
                cita.getEstilista().getId(),
                cita.getEstilista().getNombre(),
                cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(),
                cita.getNota(),
                cita.getFechaCreacion(),
                cita.getFechaActualizacion());
    }
}
