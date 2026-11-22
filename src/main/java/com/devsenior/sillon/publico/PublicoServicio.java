package com.devsenior.sillon.publico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaCreacionPeticion;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaRespuesta;
import com.devsenior.sillon.cita.CitaServicio;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioMapper;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.ServicioRespuesta;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@Service
public class PublicoServicio {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final CitaServicio citaServicio;
    private final ReglasAgenda reglasAgenda;
    private final ServicioMapper servicioMapper;
    private final Clock reloj;

    public PublicoServicio(SalonRepository salonRepository,
                           ServicioRepository servicioRepository,
                           UsuarioRepository usuarioRepository,
                           CitaRepository citaRepository,
                           CitaServicio citaServicio,
                           ReglasAgenda reglasAgenda,
                           ServicioMapper servicioMapper,
                           Clock reloj) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.citaServicio = citaServicio;
        this.reglasAgenda = reglasAgenda;
        this.servicioMapper = servicioMapper;
        this.reloj = reloj;
    }

    /**
     * Obtiene el catalogo publico del salon por su slug:
     * - Datos de contacto y ubicacion
     * - Horario en formato texto
     * - Servicios activos
     * - Estilistas activos (con sus ids de servicios; sin correos ni contrasenas)
     */
    @Transactional(readOnly = true)
    public CatalogoPublicoRespuesta obtenerCatalogo(String slug) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        String horarioTexto = formatearHorario(salon.getConfiguracion());

        List<ServicioRespuesta> servicios = servicioRepository.findBySalonIdAndActivoTrue(salon.getId()).stream()
                .map(servicioMapper::aRespuesta)
                .toList();

        List<EstilistaPublicoRespuesta> estilistas = usuarioRepository.findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo)
                .map(e -> new EstilistaPublicoRespuesta(
                        e.getId(),
                        e.getNombre(),
                        e.getServicios().stream()
                                .filter(Servicio::isActivo)
                                .map(Servicio::getId)
                                .sorted()
                                .toList()))
                .toList();

        return new CatalogoPublicoRespuesta(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                horarioTexto,
                servicios,
                estilistas);
    }

    /**
     * Consulta la disponibilidad de horarios libres de un estilista para un servicio en una fecha.
     */
    @Transactional(readOnly = true)
    public DisponibilidadRespuesta consultarDisponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado o no pertenece al salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio seleccionado no está activo.");
        }

        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(estilistaId, salon.getId(), Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado o no pertenece al salón"));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista seleccionado no está activo.");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado.");
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        List<Cita> ocupadas = citaRepository.findOcupadasPorEstilistaYFecha(
                salon.getId(),
                estilista.getId(),
                EstadoCita.estadosQueOcupanAgenda(),
                desde,
                hasta);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<LocalTime> horariosLibres = reglasAgenda.calcularHorariosLibres(
                fecha,
                servicio.getDuracionMinutos(),
                salon.getConfiguracion(),
                ocupadas,
                ahora);

        return new DisponibilidadRespuesta(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                horariosLibres);
    }

    /**
     * Reserva en linea realizada por un cliente: crea una cita PENDIENTE y con origen EN_LINEA.
     */
    @Transactional
    public CitaRespuesta reservarEnLinea(String slug, CitaCreacionPeticion peticion) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        return citaServicio.reservarEnLinea(salon.getId(), peticion);
    }

    public static String formatearHorario(ConfiguracionSalon config) {
        String dias = config.isAbreDomingos() ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        return dias + ", de " + formatearHora(config.getHoraApertura()) + " a " + formatearHora(config.getHoraCierre());
    }

    public static String formatearHora(LocalTime hora) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.of("es", "CO"));
        return hora.format(formatter).replace('\u202F', ' ').replace('\u00A0', ' ');
    }
}
