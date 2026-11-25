
package com.devsenior.sillon.publico.servicio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.cita.servicio.CitaServicio;
import com.devsenior.sillon.cita.servicio.ReglasAgenda;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaDisponibilidad;
import com.devsenior.sillon.publico.mapper.PublicoMapper;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

@Service
public class PublicoServicio {

    private static final List<EstadoCita> ESTADOS_OCUPANTES = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);

    private final SalonRepositorio salonRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final ReglasAgenda reglasAgenda;
    private final CitaServicio citaServicio;
    private final PublicoMapper publicoMapper;

    public PublicoServicio(
            SalonRepositorio salonRepositorio,
            ServicioRepositorio servicioRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            CitaRepositorio citaRepositorio,
            ReglasAgenda reglasAgenda,
            CitaServicio citaServicio,
            PublicoMapper publicoMapper) {

        this.salonRepositorio = salonRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.reglasAgenda = reglasAgenda;
        this.citaServicio = citaServicio;
        this.publicoMapper = publicoMapper;
    }

    @Transactional(readOnly = true)
    public RespuestaCatalogoPublico consultarCatalogo(String slug) {
        Salon salon = buscarSalon(slug);

        List<Servicio> servicios = servicioRepositorio.findAllBySalonIdAndActivoTrueOrderByIdAsc(
                salon.getId());

        List<Usuario> estilistas = usuarioRepositorio.buscarEstilistasActivosConServicios(
                salon.getId(), Rol.ESTILISTA);

        return publicoMapper.mapearCatalogo(salon, servicios, estilistas);
    }

    @Transactional(readOnly = true)
    public RespuestaDisponibilidad consultarDisponibilidad(
            String slug,
            Long servicioId,
            Long estilistaId,
            LocalDate fecha) {

        Salon salon = buscarSalon(slug);

        Servicio servicio = servicioRepositorio
                .findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Servicio no encontrado en el salón."));

        if (!servicio.isActivo()) {
            throw new ExcepcionConflicto("El servicio está inactivo.");
        }

        Usuario estilista = usuarioRepositorio
                .buscarEstilistaConServiciosPorId(
                        estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Estilista no encontrado en el salón."));

        if (!estilista.isActivo()) {
            throw new ExcepcionConflicto("El estilista está inactivo.");
        }

        boolean ofreceServicio = estilista.getServicios().stream()
                .anyMatch(s -> servicio.getId().equals(s.getId()));

        if (!ofreceServicio) {
            throw new ExcepcionConflicto(
                    "El estilista no ofrece el servicio solicitado.");
        }

        List<Cita> citasOcupantes = citaRepositorio.buscarCitasActivasEstilistaEnRango(
                salon.getId(),
                estilista.getId(),
                ESTADOS_OCUPANTES,
                fecha.atStartOfDay(),
                fecha.plusDays(1).atStartOfDay());

        List<LocalTime> horariosLibres = reglasAgenda.calcularHorariosLibres(
                fecha,
                servicio.getDuracionMinutos(),
                salon.getConfiguracion(),
                citasOcupantes);

        return publicoMapper.mapearDisponibilidad(
                fecha, servicio, estilista, horariosLibres);
    }

    public RespuestaCita reservar(
            String slug,
            SolicitudCrearCita solicitud) {

        return citaServicio.crearReservaPublica(slug, solicitud);
    }

    private Salon buscarSalon(String slug) {
        return salonRepositorio.findBySlug(slug)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Salón no encontrado."));
    }
}
