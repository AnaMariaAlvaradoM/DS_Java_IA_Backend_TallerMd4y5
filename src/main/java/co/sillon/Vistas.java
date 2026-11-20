package co.sillon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

final class Vistas {
    record MiSalon(long id, String nombre, String slug, String ciudad, String direccion, String telefono,
                   String plan, int creditosIa, Entradas.Configuracion configuracion) {}
    record ServicioDto(long id, String nombre, String descripcion, int duracionMinutos, BigDecimal precio) {}
    record Profesional(long id, String nombre, List<ServicioDto> servicios) {}
    record Catalogo(String nombre, String slug, String ciudad, String direccion, String telefono,
                    Entradas.Configuracion configuracion, List<ServicioDto> servicios, List<Profesional> estilistas) {}
    record ClienteDto(long id, String nombre, String telefono) {}
    record CitaDto(long id, long clienteId, String clienteNombre, String clienteTelefono,
                   long servicioId, String servicioNombre, long estilistaId, String estilistaNombre,
                   LocalDateTime inicio, LocalDateTime fin, BigDecimal precio, String estado, String origen,
                   String nota, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {}
    record SolicitudDto(long id, String clienteNombre, String clienteContacto, String mensaje, LocalDateTime fechaCreacion) {}
    record Sesion(String token, String rol, String salonSlug) {}

    static Entradas.Configuracion configuracion(Salon s) {
        return new Entradas.Configuracion(s.horaApertura,s.horaCierre,s.abreDomingos,s.intervaloMinutos);
    }
    static MiSalon salon(Salon s) {
        return new MiSalon(s.id,s.nombre,s.slug,s.ciudad,s.direccion,s.telefono,s.plan,s.creditosIa,configuracion(s));
    }
    static ServicioDto servicio(Servicio s) {
        return new ServicioDto(s.id,s.nombre,s.descripcion,s.duracionMinutos,s.precio);
    }
    static Profesional profesional(Usuario u) {
        return new Profesional(u.id,u.nombre,u.servicios.stream().sorted(java.util.Comparator.comparing(s -> s.id))
                .map(Vistas::servicio).toList());
    }
    static CitaDto cita(Cita c) {
        return new CitaDto(c.id,c.cliente.id,c.cliente.nombre,c.cliente.telefono,c.servicio.id,c.servicio.nombre,
                c.estilista.id,c.estilista.nombre,c.inicio,c.fin,c.precio,c.estado,c.origen,c.nota,c.fechaCreacion,c.fechaActualizacion);
    }
}
