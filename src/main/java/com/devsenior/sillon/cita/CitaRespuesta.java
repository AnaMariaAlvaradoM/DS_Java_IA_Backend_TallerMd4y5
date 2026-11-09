package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

public record CitaRespuesta(Long id, LocalDateTime inicio, LocalDateTime fin, EstadoCita estado,
        OrigenCita origen, Integer precio, Long servicioId, String servicioNombre,
        Long estilistaId, String estilistaNombre, String clienteNombre, String clienteTelefono,
        String nota, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
    public static CitaRespuesta desde(Cita cita) {
        return new CitaRespuesta(cita.getId(), cita.getInicio(), cita.getFin(), cita.getEstado(),
                cita.getOrigen(), cita.getPrecio(), cita.getServicio().getId(), cita.getServicio().getNombre(),
                cita.getEstilista().getId(), cita.getEstilista().getNombre(), cita.getCliente().getNombre(),
                cita.getCliente().getTelefono(), cita.getNota(), cita.getFechaCreacion(), cita.getFechaActualizacion());
    }
}
