package com.devsenior.sillon.publico;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaServicio;
import com.devsenior.sillon.cita.CitaSolicitud;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class PublicoServicio {

    private static final String MENSAJE_SALON_NO_ENCONTRADO = "Salón no encontrado";
    private static final String MENSAJE_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado";
    private static final String MENSAJE_ESTILISTA_NO_ENCONTRADO = "Estilista no encontrado";

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final CitaServicio citaServicio;
    private final ReglasAgenda reglasAgenda;
    private final Clock reloj;

    public PublicoServicio(SalonRepository salonRepository, ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository, CitaRepository citaRepository,
            CitaServicio citaServicio, ReglasAgenda reglasAgenda, Clock reloj) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.citaServicio = citaServicio;
        this.reglasAgenda = reglasAgenda;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public CatalogoPublicoRespuesta catalogo(String slug) {
        Salon salon = buscarSalon(slug);
        List<CatalogoPublicoRespuesta.ServicioPublico> servicios = servicioRepository
                .findBySalonId(salon.getId()).stream()
                .filter(Servicio::isActivo)
                .sorted(Comparator.comparing(Servicio::getId))
                .map(servicio -> new CatalogoPublicoRespuesta.ServicioPublico(
                        servicio.getId(), servicio.getNombre(),
                        servicio.getDuracionMinutos(), servicio.getPrecio()))
                .toList();
        List<CatalogoPublicoRespuesta.EstilistaPublico> estilistas = usuarioRepository
                .findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo)
                .sorted(Comparator.comparing(Usuario::getId))
                .map(estilista -> new CatalogoPublicoRespuesta.EstilistaPublico(
                        estilista.getId(), estilista.getNombre(),
                        estilista.getServicios().stream().map(Servicio::getId).toList()))
                .toList();
        return new CatalogoPublicoRespuesta(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                textoHorario(salon.getConfiguracion()),
                servicios,
                estilistas);
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta disponibilidad(String slug, Long servicioId, Long estilistaId,
            LocalDate fecha) {
        Salon salon = buscarSalon(slug);
        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SERVICIO_NO_ENCONTRADO));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }
        Usuario estilista = usuarioRepository.findById(estilistaId)
                .filter(usuario -> usuario.getRol() == Rol.ESTILISTA)
                .filter(usuario -> usuario.getSalon().getId().equals(salon.getId()))
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_ESTILISTA_NO_ENCONTRADO));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }
        if (!estilista.ofrece(servicio)) {
            throw new ReglaNegocioException("El estilista no ofrece este servicio");
        }
        ConfiguracionSalon configuracion = salon.getConfiguracion();
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY && !configuracion.isAbreDomingos()) {
            throw new ReglaNegocioException("El salón no abre los domingos (horario: de "
                    + configuracion.getHoraApertura() + " a " + configuracion.getHoraCierre() + ")");
        }
        List<EstadoCita> estadosQueOcupan = Arrays.stream(EstadoCita.values())
                .filter(EstadoCita::ocupaAgenda)
                .toList();
        List<Cita> ocupadas = citaRepository.citasQueOcupan(salon.getId(), estilista.getId(),
                estadosQueOcupan, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<String> horariosLibres = reglasAgenda
                .calcularHorariosLibres(configuracion, fecha, servicio.getDuracionMinutos(),
                        ocupadas, ahora)
                .stream()
                .map(inicio -> inicio.toLocalTime().toString())
                .toList();
        return new DisponibilidadRespuesta(fecha, servicio.getNombre(), estilista.getNombre(),
                servicio.getDuracionMinutos(), horariosLibres);
    }

    @Transactional
    public CitaRespuesta reservar(String slug, CitaSolicitud solicitud) {
        Salon salon = buscarSalon(slug);
        return citaServicio.reservarEnLinea(salon.getId(), solicitud);
    }

    private Salon buscarSalon(String slug) {
        return salonRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SALON_NO_ENCONTRADO));
    }

    private static String textoHorario(ConfiguracionSalon configuracion) {
        String dias = configuracion.isAbreDomingos() ? "Todos los días"
                : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + formatoHora(configuracion.getHoraApertura()) + " a "
                + formatoHora(configuracion.getHoraCierre());
    }

    private static String formatoHora(LocalTime hora) {
        int hora12 = hora.getHour() % 12;
        if (hora12 == 0) {
            hora12 = 12;
        }
        String sufijo = hora.getHour() < 12 ? "a. m." : "p. m.";
        return hora12 + ":" + String.format("%02d", hora.getMinute()) + " " + sufijo;
    }
}
