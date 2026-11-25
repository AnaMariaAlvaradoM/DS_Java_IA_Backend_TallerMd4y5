
package com.devsenior.sillon.publico.mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaDisponibilidad;
import com.devsenior.sillon.publico.dto.RespuestaEstilistaPublico;
import com.devsenior.sillon.publico.dto.RespuestaServicioPublico;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.modelo.Usuario;

@Component
public class PublicoMapper {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter FORMATO_SLOT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RespuestaCatalogoPublico mapearCatalogo(
            Salon salon,
            List<Servicio> servicios,
            List<Usuario> estilistas) {

        List<Servicio> serviciosPublicos = servicios.stream()
                .filter(Servicio::isActivo)
                .filter(servicio -> servicio.getSalon() != null
                        && salon.getId().equals(servicio.getSalon().getId()))
                .toList();

        Set<Long> idsServiciosPublicos = serviciosPublicos.stream()
                .map(Servicio::getId)
                .collect(Collectors.toSet());

        List<RespuestaServicioPublico> respuestaServicios = serviciosPublicos.stream()
                .map(this::mapearServicio)
                .toList();

        List<RespuestaEstilistaPublico> respuestaEstilistas = estilistas.stream()
                .filter(Usuario::isActivo)
                .filter(usuario -> usuario.getSalon() != null
                        && salon.getId().equals(usuario.getSalon().getId()))
                .map(estilista -> mapearEstilista(
                        estilista, idsServiciosPublicos))
                .toList();

        return new RespuestaCatalogoPublico(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                formatearHorario(salon.getConfiguracion()),
                respuestaServicios,
                respuestaEstilistas);
    }

    public RespuestaDisponibilidad mapearDisponibilidad(
            LocalDate fecha,
            Servicio servicio,
            Usuario estilista,
            List<LocalTime> horariosLibres) {

        List<String> horariosFormateados = horariosLibres.stream()
                .map(hora -> hora.format(FORMATO_SLOT))
                .toList();

        return new RespuestaDisponibilidad(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                horariosFormateados);
    }

    private RespuestaServicioPublico mapearServicio(Servicio servicio) {
        return new RespuestaServicioPublico(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getPrecio());
    }

    private RespuestaEstilistaPublico mapearEstilista(
            Usuario estilista,
            Set<Long> idsServiciosPublicos) {

        List<Long> servicioIds = estilista.getServicios().stream()
                .filter(Servicio::isActivo)
                .map(Servicio::getId)
                .filter(idsServiciosPublicos::contains)
                .distinct()
                .sorted()
                .toList();

        return new RespuestaEstilistaPublico(
                estilista.getId(),
                estilista.getNombre(),
                servicioIds);
    }

    private String formatearHorario(ConfiguracionSalon configuracion) {
        String apertura = configuracion.getHoraApertura()
                .format(FORMATO_HORA);

        String cierre = configuracion.getHoraCierre()
                .format(FORMATO_HORA);

        String dias = configuracion.isAbreDomingos()
                ? "lunes a domingo"
                : "lunes a sábado";

        return apertura + " - " + cierre + " (" + dias + ")";
    }
}
