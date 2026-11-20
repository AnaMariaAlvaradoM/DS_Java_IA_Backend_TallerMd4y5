package com.devsenior.sillon.publico;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.CitaRepository;
import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.cita.dto.CitaRespuesta;
import com.devsenior.sillon.cita.dto.CrearCitaRequest;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.publico.dto.CatalogoSalonRespuesta;
import com.devsenior.sillon.publico.dto.DisponibilidadRespuesta;
import com.devsenior.sillon.publico.dto.EstilistaPublicoDTO;
import com.devsenior.sillon.publico.dto.ServicioPublicoDTO;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class PublicoService {

    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final ReglasAgenda reglasAgenda;
    private final Clock clock;

    public PublicoService(
            SalonRepository salonRepository,
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            CitaService citaService,
            ReglasAgenda reglasAgenda,
            Clock clock
    ) {
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.reglasAgenda = reglasAgenda;
        this.clock = clock;
    }

    public static String formatearHorario(ConfiguracionSalon config) {
        if (config == null) {
            return "";
        }
        String dias = config.isAbreDomingos() ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        return String.format("%s, de %s a %s", dias, formatearHora(config.getHoraApertura()), formatearHora(config.getHoraCierre()));
    }

    private static String formatearHora(LocalTime hora) {
        int hora12 = hora.getHour() % 12;
        if (hora12 == 0) hora12 = 12;
        String amPm = hora.getHour() < 12 ? "a. m." : "p. m.";
        return String.format("%d:%02d %s", hora12, hora.getMinute(), amPm);
    }

    @Transactional(readOnly = true)
    public CatalogoSalonRespuesta obtenerCatalogo(String slug) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        List<ServicioPublicoDTO> servicios = servicioRepository.findBySalonIdAndActivoTrue(salon.getId()).stream()
                .map(s -> new ServicioPublicoDTO(s.getId(), s.getNombre(), s.getDescripcion(), s.getDuracionMinutos(), s.getPrecio()))
                .toList();

        List<EstilistaPublicoDTO> estilistas = usuarioRepository.findBySalonIdAndRol(salon.getId(), Rol.ESTILISTA).stream()
                .filter(Usuario::isActivo)
                .map(u -> new EstilistaPublicoDTO(
                        u.getId(),
                        u.getNombre(),
                        u.getServicios().stream().map(Servicio::getId).toList()
                ))
                .toList();

        return new CatalogoSalonRespuesta(
                salon.getNombre(),
                salon.getSlug(),
                salon.getCiudad(),
                salon.getDireccion(),
                salon.getTelefono(),
                formatearHorario(salon.getConfiguracion()),
                servicios,
                estilistas
        );
    }

    @Transactional(readOnly = true)
    public DisponibilidadRespuesta consultarDisponibilidad(String slug, Long servicioId, Long estilistaId, LocalDate fecha) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Servicio servicio = servicioRepository.findByIdAndSalonId(servicioId, salon.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado en este salón"));

        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio no está activo");
        }

        Usuario estilista = usuarioRepository.findById(estilistaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado"));

        if (!estilista.getSalon().getId().equals(salon.getId())) {
            throw new RecursoNoEncontradoException("Estilista no pertenece a este salón");
        }

        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista no está activo");
        }

        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado");
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        List<EstadoCita> estadosOcupan = List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
        List<Cita> citasOcupadas = citaRepository.findCitasOcupadasEstilistaEnDia(salon.getId(), estilista.getId(), estadosOcupan, desde, hasta);

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<LocalTime> horariosLibres = reglasAgenda.calcularHorariosLibres(
                fecha,
                servicio.getDuracionMinutos(),
                salon.getConfiguracion(),
                citasOcupadas,
                ahora
        );

        return new DisponibilidadRespuesta(
                fecha,
                servicio.getNombre(),
                estilista.getNombre(),
                servicio.getDuracionMinutos(),
                horariosLibres
        );
    }

    @Transactional
    public CitaRespuesta crearReservaEnLinea(String slug, CrearCitaRequest request) {
        Salon salon = salonRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        Cita cita = citaService.agendarInterno(salon.getId(), request, OrigenCita.EN_LINEA, EstadoCita.PENDIENTE);
        return citaService.mapearARespuesta(cita);
    }
}
