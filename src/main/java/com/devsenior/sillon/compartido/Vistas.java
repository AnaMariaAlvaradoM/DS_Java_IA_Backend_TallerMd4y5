package com.devsenior.sillon.compartido;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Vistas {
    public record MiSalon(
            long id,
            String nombre,
            String slug,
            String ciudad,
            String direccion,
            String telefono,
            String plan,
            int creditosIa,
            Entradas.Configuracion configuracion,
            LocalDateTime fechaCreacion) {}

    public record ServicioDto(
            long id,
            String nombre,
            String descripcion,
            int duracionMinutos,
            BigDecimal precio,
            boolean activo) {}

    public record PublicoProfesional(long id, String nombre, List<Long> servicioIds) {}

    public record Profesional(
            long id, String nombre, String email, boolean activo, List<String> servicios) {}

    public record Catalogo(
            String nombre,
            String slug,
            String ciudad,
            String direccion,
            String telefono,
            String horario,
            List<ServicioDto> servicios,
            List<PublicoProfesional> estilistas) {}

    public record ClienteDto(long id, String nombre, String telefono) {}

    public record CitaDto(
            long id,
            long clienteId,
            String clienteNombre,
            String clienteTelefono,
            long servicioId,
            String servicioNombre,
            long estilistaId,
            String estilistaNombre,
            LocalDateTime inicio,
            LocalDateTime fin,
            BigDecimal precio,
            String estado,
            String origen,
            String nota,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion) {}

    public record SolicitudDto(
            long id,
            String clienteNombre,
            String clienteContacto,
            String mensajes,
            String motivo,
            String estado,
            LocalDateTime fechaCreacion) {}

    public record Sesion(
            String token,
            String tipo,
            int expiraEnMinutos,
            long usuarioId,
            String nombre,
            String rol,
            long salonId,
            String salonSlug) {}

    public static Entradas.Configuracion configuracion(Salon s) {
        return new Entradas.Configuracion(
                s.getHoraApertura(),
                s.getHoraCierre(),
                s.getAbreDomingos(),
                s.getIntervaloMinutos());
    }

    public static MiSalon salon(Salon s) {
        return new MiSalon(
                s.getId(),
                s.getNombre(),
                s.getSlug(),
                s.getCiudad(),
                s.getDireccion(),
                s.getTelefono(),
                s.getPlan(),
                s.getCreditosIa(),
                configuracion(s),
                s.getFechaCreacion());
    }

    public static ServicioDto servicio(Servicio s) {
        return new ServicioDto(
                s.getId(),
                s.getNombre(),
                s.getDescripcion(),
                s.getDuracionMinutos(),
                s.getPrecio(),
                s.getActivo());
    }

    public static Profesional profesional(Usuario u) {
        return new Profesional(
                u.getId(),
                u.getNombre(),
                u.getEmail(),
                u.getActivo(),
                u.getServicios().stream()
                        .sorted(java.util.Comparator.comparing(s -> s.getId()))
                        .map(s -> s.getNombre())
                        .toList());
    }

    public static CitaDto cita(Cita c) {
        return new CitaDto(
                c.getId(),
                c.getCliente().getId(),
                c.getCliente().getNombre(),
                c.getCliente().getTelefono(),
                c.getServicio().getId(),
                c.getServicio().getNombre(),
                c.getEstilista().getId(),
                c.getEstilista().getNombre(),
                c.getInicio(),
                c.getFin(),
                c.getPrecio(),
                c.getEstado(),
                c.getOrigen(),
                c.getNota(),
                c.getFechaCreacion(),
                c.getFechaActualizacion());
    }
}
