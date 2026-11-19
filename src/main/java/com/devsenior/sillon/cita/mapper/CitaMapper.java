package com.devsenior.sillon.cita.mapper;

import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.model.Cita;
import org.springframework.stereotype.Component;

/** Se llama dentro del método transaccional del servicio (las relaciones son perezosas). */
@Component
public class CitaMapper {

    public CitaResponse toResponse(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getInicio(),
                cita.getFin(),
                cita.getEstado(),
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
                cita.getFechaActualizacion());
    }
}
